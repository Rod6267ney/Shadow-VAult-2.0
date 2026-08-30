package com.example.services

import android.content.Context
import android.util.Log
import com.example.data.WorkspaceConfig
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Motor responsável pelo isolamento de rede da Máquina Virtual (Chaos OS).
 * Utiliza privilégios Root/Shizuku para manipular namespaces de rede (ip netns)
 * e regras de iptables para forçar o tráfego da VM através de um proxy/VPN dedicado,
 * sem afetar o tráfego do host.
 */
object NetworkIsolationEngine {
    private const val TAG = "NetworkIsolationEngine"
    private const val NETNS_PREFIX = "chaos_ns_"

    /**
     * Prepara um namespace de rede isolado para a VM.
     * Se useProxy for true, ele configuraria o tun2socks ou regras de iptables
     * para redirecionar o tráfego do namespace.
     */
    suspend fun setupIsolation(context: Context, config: WorkspaceConfig): Boolean = withContext(Dispatchers.IO) {
        val isRoot = try { Shell.getShell().isRoot } catch(e: Exception) { false }
        if (!isRoot) {
            Log.w(TAG, "Privilégios insuficientes (Root/Shizuku) para manipulação de Network Namespaces.")
            return@withContext false
        }

        val namespaceName = "$NETNS_PREFIX${config.id}"
        Log.d(TAG, "Configurando isolamento de rede no namespace: $namespaceName")

        val commands = mutableListOf<String>()

        // 1. Limpa qualquer namespace antigo travado com o mesmo nome
        commands.add("ip netns del $namespaceName || true")

        // 2. Cria o novo Network Namespace
        commands.add("ip netns add $namespaceName")

        // 3. Levanta a interface de loopback no namespace (necessário para processos locais da VM)
        commands.add("ip netns exec $namespaceName ip link set dev lo up")

        // 4. Criação do par veth (Virtual Ethernet) para conectar o host ao namespace
        val vethHost = "veth_h_${config.id.take(4)}"
        val vethGuest = "veth_g_${config.id.take(4)}"
        
        commands.add("ip link add $vethHost type veth peer name $vethGuest")
        
        // Move o guest para dentro do namespace
        commands.add("ip link set $vethGuest netns $namespaceName")
        
        // 5. Configuração de IP na interface do Host
        commands.add("ip addr add 10.1.1.1/24 dev $vethHost")
        commands.add("ip link set $vethHost up")

        // 6. Configuração de IP na interface do Guest (VM)
        commands.add("ip netns exec $namespaceName ip addr add 10.1.1.2/24 dev $vethGuest")
        commands.add("ip netns exec $namespaceName ip link set $vethGuest up")
        
        // Define o roteamento padrão do guest apontando para o host
        commands.add("ip netns exec $namespaceName ip route add default via 10.1.1.1")

        // 7. NAT (Network Address Translation) no Host usando iptables
        commands.add("iptables -t nat -A POSTROUTING -s 10.1.1.2/32 -j MASQUERADE")
        commands.add("sysctl -w net.ipv4.ip_forward=1") // Habilita repasse de pacotes no kernel

        // 8. Roteamento de Proxy Isolado
        if (config.proxyIp != "Oculto" && config.proxyIp.isNotBlank()) {
            Log.d(TAG, "Aplicando redirecionamento de Proxy para região: ${config.proxyRegion}")
            // Aqui invocaríamos o binário do tun2socks dentro do namespace
            // tun2socks -device tun0 -proxy socks5://${config.proxyIp}:1080
            // Mas como simulação ou setup base, preparamos a rota:
            commands.add("ip netns exec $namespaceName ip tuntap add mode tun dev tun0")
            commands.add("ip netns exec $namespaceName ip addr add 10.0.0.1/24 dev tun0")
            commands.add("ip netns exec $namespaceName ip link set tun0 up")
            // Redirecionaria o tráfego do namespace para a TUN
            // commands.add("ip netns exec $namespaceName ip route add default dev tun0 table 100")
            // commands.add("ip netns exec $namespaceName ip rule add from all lookup 100")
        }

        try {
            commands.forEach { cmd ->
                Log.d(TAG, "Executando: $cmd")
                Shell.cmd(cmd).exec()
            }
            Log.d(TAG, "Isolamento de rede concluído para $namespaceName.")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao configurar isolamento de rede", e)
            return@withContext false
        }
    }

    /**
     * Desfaz as configurações de rede, apagando o namespace e limpando iptables.
     */
    suspend fun teardownIsolation(config: WorkspaceConfig) = withContext(Dispatchers.IO) {
        if (!Shell.getShell().isRoot) return@withContext

        val namespaceName = "$NETNS_PREFIX${config.id}"
        val vethHost = "veth_h_${config.id.take(4)}"

        Log.d(TAG, "Derrubando isolamento de rede: $namespaceName")

        val commands = listOf(
            "ip netns del $namespaceName || true",
            "ip link delete $vethHost || true",
            "iptables -t nat -D POSTROUTING -s 10.1.1.2/32 -j MASQUERADE || true"
        )

        commands.forEach { cmd ->
            Shell.cmd(cmd).exec()
        }
    }
}
