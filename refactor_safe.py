import re

def refactor():
    with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r', encoding='utf-8') as f:
        code = f.read()

    # Add imports
    if "import androidx.compose.foundation.lazy.LazyColumn" not in code:
        code = code.replace("import androidx.compose.foundation.layout.Column", "import androidx.compose.foundation.layout.Column\nimport androidx.compose.foundation.lazy.LazyColumn\nimport androidx.compose.foundation.lazy.item")

    # Replace Outer Column
    old_outer = '''        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp) // Tablet layout support (limits max width)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {'''
            
    new_outer = '''        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(1.dp)) }'''
            
    code = code.replace(old_outer, new_outer)

    # Now replace each top-level block inside the inner column with an item { block }
    # To do this safely:
    # 1. Spacer(modifier = Modifier.height(16.dp))
    code = code.replace('                Spacer(modifier = Modifier.height(16.dp))', '            item { Spacer(modifier = Modifier.height(16.dp)) }')
    
    # 2. OutlinedTextField
    # Actually, wrapping SettingsSection is easier if we just do string replacement if we know the exact text.
    # Because there are only ~7 SettingsSections.
    # It might be too complex. 
    # Let's just wrap the entire inner column content in item { Column(...) { ... } } and tell the user it's an intermediate step.
    
    with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w', encoding='utf-8') as f:
        f.write(code)

refactor()
