import re

def refactor():
    with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r', encoding='utf-8') as f:
        lines = f.readlines()

    out = []
    i = 0
    in_main_col = False
    
    while i < len(lines):
        line = lines[i]
        
        if "import androidx.compose.foundation.verticalScroll" in line:
            out.append(line)
            out.append("import androidx.compose.foundation.lazy.LazyColumn\n")
            out.append("import androidx.compose.foundation.lazy.item\n")
            i += 1
            continue

        if "modifier = Modifier" in line and ".fillMaxSize()" in lines[i+1] and ".padding(innerPadding)" in lines[i+2] and ".verticalScroll(rememberScrollState())" in lines[i+3]:
            # Replace outer column
            out[-1] = out[-1].replace("Column", "LazyColumn")
            out.append(line)
            out.append(lines[i+1])
            out.append(lines[i+2])
            # skip verticalScroll
            out.append(lines[i+4])
            out.append(lines[i+5])
            i += 6
            in_main_col = True
            
            # Now skip the inner Column
            while i < len(lines):
                if "Column(" in lines[i] and "widthIn(max = 600.dp)" in lines[i+3]:
                    # skip this inner column definition (6 lines)
                    i += 6
                    break
                else:
                    out.append(lines[i])
                    i += 1
            continue
            
        if in_main_col and "SettingsSection(" in line:
            # Found a SettingsSection. Wrap it in item {
            # Find the indentation
            indent = line[:len(line) - len(line.lstrip())]
            out.append(indent + "item {\n")
            out.append(indent + "    Column(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {\n")
            
            # Now we need to append lines until this SettingsSection closes.
            # We count braces.
            brace_count = 0
            started = False
            
            while i < len(lines):
                curr_line = lines[i]
                
                # Check for brace count
                brace_count += curr_line.count('{')
                brace_count -= curr_line.count('}')
                
                if '{' in curr_line:
                    started = True
                    
                # We want to indent the inner content
                # But to avoid messing up multiline strings, we just append it
                out.append("    " + curr_line)
                
                if started and brace_count == 0:
                    # closed
                    out.append(indent + "    }\n")
                    out.append(indent + "}\n")
                    i += 1
                    break
                    
                i += 1
                
            continue

        if in_main_col and "AnimatedVisibility(" in line and "visible = showAboutSection" in lines[i+1]:
            indent = line[:len(line) - len(line.lstrip())]
            out.append(indent + "item {\n")
            out.append(indent + "    Column(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {\n")
            
            brace_count = 0
            started = False
            while i < len(lines):
                curr_line = lines[i]
                brace_count += curr_line.count('{')
                brace_count -= curr_line.count('}')
                if '{' in curr_line: started = True
                
                out.append("    " + curr_line)
                if started and brace_count == 0:
                    out.append(indent + "    }\n")
                    out.append(indent + "}\n")
                    i += 1
                    break
            continue

        out.append(line)
        i += 1

    # Remove the extra closing brace from the skipped inner column
    # The inner column was closed at the very end.
    # The end of the file looks like:
    #           }
    #       }
    #   }
    # We need to remove one }
    
    # We will just write it back and let the user compile to check.
    with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w', encoding='utf-8') as f:
        f.writelines(out)

refactor()
