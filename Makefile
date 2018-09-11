all: dist

fragments/fragments.txt: fragments.sh
	mkdir -p fragments
	./fragments.sh
	touch fragments/fragments.txt

document.html: document.md fragments/fragments.txt
	./pandoc.sh document.md > document.html.tmp
	mv document.html.tmp document.html

dist: document.html
	mkdir -p dist
	mv document.html dist
	cp document.css dist
	cp normal_nat.png dist
	find dist -type d -exec chmod -v 755 {} \;
	find dist -type f -exec chmod -v 644 {} \;

clean:
	rm -f document.html document.html.tmp
	rm -rf fragments

