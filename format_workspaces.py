import re

with open('app/src/main/java/com/example/ui/screens/WorkspacesScreen.kt', 'r') as f:
    content = f.read()

# Add imports if missing
imports_to_add = """
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
"""
content = content.replace('import androidx.compose.ui.unit.sp', 'import androidx.compose.ui.unit.sp\n' + imports_to_add)

# Find WorkspacesScreen Composable
ws_regex = r'(@Composable\nfun WorkspacesScreen\(\)\s*\{)(.*?)(var showAppSelectorForWorkspace by remember \{ mutableStateOf<WorkspaceConfig\?>\(null\) \})'
match = re.search(ws_regex, content, re.DOTALL)
if match:
    new_state = """    var showCreateDialog by remember { mutableStateOf(false) }
    var newWorkspaceName by remember { mutableStateOf("") }
    var showDeleteDialogFor by remember { mutableStateOf<WorkspaceConfig?>(null) }
"""
    content = content[:match.end()] + "\n" + new_state + content[match.end():]

# We should add FloatingActionButton to the Scaffold or wrap it if there's no Scaffold.
# The screen likely has a Column
col_regex = r'(if \(isLoading\) \{\n\s*CircularProgressIndicator\(\)\n\s*\} else \{\n\s*LazyColumn\(modifier = Modifier\.fillMaxSize\(\)\) \{)'
# Let's check what the layout is
