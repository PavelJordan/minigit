#!/bin/bash

mvn clean package

mkdir -p ~/.local/lib/minigit/
mkdir -p ~/.local/bin/
cp target/minigit.jar ~/.local/lib/minigit/

echo '#!/bin/bash' > ~/.local/bin/minigit
echo 'java -jar ~/.local/lib/minigit/minigit.jar "$@"' >> ~/.local/bin/minigit
chmod +x ~/.local/bin/minigit

echo "MiniGit is installed! You can run the minigit command now."

if ! echo ":$PATH:" | grep -q ":$HOME/.local/bin:"; then
    echo 'Note: ~/.local/bin is not on your PATH. Add it by running:'
    echo '  echo '\''export PATH="$HOME/.local/bin:$PATH"'\'' >> ~/.zshrc && source ~/.zshrc'
fi
