.PHONY: all pdf html clean

all: geworfen.pdf geworfen.html

# org → acmart LaTeX → PDF. build.el = memex-kb paper_build.el 벤더링(변환기 SSOT).
# texlive 필요: nix develop (flake.nix) 안에서 make, 또는 PATH에 texlive+emacs.
pdf: geworfen.pdf
geworfen.pdf: geworfen.org build.el geworfen.bib
	emacs -Q --batch --script build.el geworfen.org

# org → html5 (pandoc --citeproc + MathJax, emacs/texlive 불필요).
# 변환 로직 SSOT = memex-kb paper2org-html (동일 플래그). 우리는 저자라 import 단계 없음.
html: geworfen.html
geworfen.html: geworfen.org geworfen.bib
	pandoc -f org -t html5 -s --citeproc --bibliography=geworfen.bib --mathjax -o geworfen.html geworfen.org

clean:
	rm -f *.aux *.bbl *.blg *.fdb_latexmk *.fls *.log *.out *.toc *~
	# 산출물(.pdf/.tex/.html)은 재현성 위해 수동 삭제 (git 커밋 대상일 수 있음)
