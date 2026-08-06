import re

# 1. Revert Manifest
with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

content = re.sub(r'<service android:name="\.vpn\.ChaosVpnService".*?</service>', '', content, flags=re.DOTALL)

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)

# 2. Revert WorkspacesScreen.kt
with open('app/src/main/java/com/example/ui/screens/WorkspacesScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('com.example.vpn.VpnManager.enableWorkspaceVpn(context, id, selectedProxyRegion)', '')

with open('app/src/main/java/com/example/ui/screens/WorkspacesScreen.kt', 'w') as f:
    f.write(content)
