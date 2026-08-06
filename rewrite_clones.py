import os

with open('app/src/main/java/com/example/ui/screens/ClonesScreen.kt', 'r') as f:
    original = f.read()

# We extract everything BEFORE AppSelectionWizard
match = original.find('fun AppSelectionWizard')
if match == -1:
    # try to find the start of the broken part
    match = original.find('@androidx.compose.material3.ExperimentalMaterial3Api')

if match != -1:
    clean_top = original[:match]
else:
    clean_top = original

# Ensure imports are good
imports = """
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ChevronRight
import com.example.ui.theme.*
"""

# add imports if not present
if "import android.content.Context" not in clean_top:
    clean_top = clean_top.replace("import android.app.Application", "import android.app.Application\n" + imports)

# Make sure to remove any trailing junk
last_brace = clean_top.rfind('}')
if last_brace != -1:
    clean_top = clean_top[:last_brace+1]

with open('app/src/main/java/com/example/ui/screens/ClonesScreen.kt', 'w') as f:
    f.write(clean_top)
    f.write("\n\n")

