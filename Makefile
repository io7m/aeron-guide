all: guide.html

fragments: fragments.sh
	mkdir -p fragments
	./fragments.sh
	touch fragments/fragments.txt

guide.html: guide.md fragments
	./pandoc.sh guide.md > guide.html.tmp
	mv guide.html.tmp guide.html

clean:
	rm -f guide.html guide.html.tmp
	rm -rf fragments

