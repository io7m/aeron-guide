all: guide.html

out: fragments.sh
	./fragments.sh

guide.html: guide.md out
	./pandoc.sh guide.md > guide.html.tmp
	mv guide.html.tmp guide.html

clean:
	rm -f guide.html guide.html.tmp
	rm -rf out
