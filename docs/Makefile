.PHONY: all sample main clean

all: main.pdf

main.pdf: main.org build.el main.bib
	emacs -Q --batch --script build.el main.org

sample: sample.pdf

sample.pdf: sample.org build.el sample.bib
	emacs -Q --batch --script build.el sample.org

clean:
	rm -f *.aux *.bbl *.blg *.fdb_latexmk *.fls *.log *.out *.toc *.tex *~
	# PDF는 수동 삭제 (git에 넣을 수 있으므로)
