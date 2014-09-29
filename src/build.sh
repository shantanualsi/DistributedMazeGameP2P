#compile the code
javac play/*.java
javac game/*.java
javac client/*.java
javac server/*.java

#Package into jar
rm -rf game.jar
jar cvfm game.jar manifest.txt play/*.class game/*.class client/*.class server/*.class

