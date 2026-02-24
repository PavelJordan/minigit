#!/bin/bash

mvn clean package

mkdir -p ~/.local/lib/minigit/
cp target/minigit.jar ~/.local/lib/minigit/

echo '#!/bin/bash' > ~/.local/bin/minigit
echo 'java -jar ~/.local/lib/minigit/minigit.jar "$@"' >> ~/.local/bin/minigit
chmod +x ~/.local/bin/minigit

echo "MiniGit is installed! You can run the minigit command now."
