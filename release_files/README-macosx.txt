In order to run the application you'll have to open a terminal, navigate to the directory where you extracted jnodes and run the following commands:

find /path/to/your/directory -xattrname com.apple.quarantine -perm -u+w -print0 | xargs -0 xattr -d com.apple.quarantine
chmod +x run_with_console.command

Then double clicking the run_with_console.command should work.
