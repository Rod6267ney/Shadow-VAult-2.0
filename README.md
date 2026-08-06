# ShadowVault: Infinite Workspace Engine

Este projeto implementa uma arquitetura robusta e nativa de clonagem de aplicativos para Android utilizando **Work Profiles** (Perfis de Trabalho) via Shizuku.

Diferente de soluções de virtualização com C++ que exigem hooks profundos no sistema, o ShadowVault utiliza as próprias APIs nativas do Android (ActivityManager e UserManager) para garantir isolamento perfeito de dados, criptografia e estabilidade para aplicativos complexos como o WhatsApp.

## Motor de Bypass de Limites (Brute Force Engine)

O Android normalmente impõe um limite estrito de Perfis de Trabalho (geralmente 1 por usuário) e de contas secundárias. O ShadowVault implementa um motor inteligente para quebrar esses limites e criar **ambientes infinitos**:

1. **Injeção de Sysprops**: Modificação de propriedades de sistema (`fw.max_users`, `persist.sys.max_profiles`) via shell interativo para forçar o sistema a aceitar mais instâncias.
2. **Cascata de Tipos (Profile Strategies)**: O motor tenta criar os contêineres utilizando todos os sub-tipos de perfis conhecidos (`MANAGED`, `CLONE`, `PRIVATE`, `GUEST`, `RESTRICTED`, `SECONDARY`).
3. **Aninhamento (Nested Sandbox)**: Quando o Usuário 0 (Principal) atinge o limite máximo *hardcoded* do Kernel, o ShadowVault cria silenciosamente um "Usuário Fantasma" (Ghost User) operando em background e, em seguida, aninha um novo Perfil de Trabalho **dentro** desse usuário fantasma. Isso multiplica exponencialmente a capacidade de criação de clones, permitindo bypasses em ROMs altamente restritivas como HyperOS (Xiaomi) e OneUI (Samsung).
4. **Isolamento Absoluto**: Cada instância roda com seu próprio `/data/user/X`, garantindo que os clones operem 100% isolados, com hardware spoofado (Fake ID, Fake GPS) e blindagem contra leitura de dados cruzada.

## Como Executar

O aplicativo já está configurado. Inicie o app via AI Studio, garanta as permissões do Shizuku, e comece a criar instâncias infinitas na aba "Vault".
