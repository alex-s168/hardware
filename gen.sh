rm -rf build
scala-cli src/*
cat build/*.sv > build/__all__.sv

