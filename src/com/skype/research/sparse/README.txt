Expands a "sparse" firmware partition image into a mountable bit-exact disk
image. This is normally needed to extract the contents of /system/(lib|bin)
from a firmware deliverable.

USAGE

 java -jar bakebread.jar -0 sparse-image.img mountable-image.img
    # expand a single-file image

 java -jar bakebread.jar -0 [image.sparsechunk.N]... mountable-image.img
    # expand a multi-chunk image

 java -jar bakebread.jar -0
    # display a help page

SCANNING

Sometimes expanded images are prepended with a signature or other metadata.
In this case Desparser attempts a search for the primary file system block.
If such a block is found at position 0, this is considered a perfect match.
Otherwise, the following heuristics are considered:
 - whether this is a unique occurrence in a chunk:
        30% / matches in the same data chunk
 - whether the position is "round":
        30% * position trailing zeroes / block size trailing zeroes
 - whether this is an early occurrence:
        20% / 1-based rank
 - whether the position is "mentioned" near the file start:
        12% for a (seeming) absolute file offset
         8% for a (seeming) relative file offset

OPTIONS

File system selection options:
 -F4, --fs=ext4                 EXT4 (default / the only one supported ATM)
 
Superblock scanning options:
 -Ss <kb>, --scan-super=<kb>    Scan that far from start for the superblock.
                                    Default is 1024K (1 megabyte).
                                    
 -Si <kb>, --scan-index=<kb>    Scan that far from start for file metadata.
                                    Just one of many advisory heuristics.
                                        Default is 16K.

 -St <%>, --scan-threshold=<%>  A rating threshold for a superblock candidate
                                    to be displayed. Default is 10 (%).
                                    
 -Sc <id>, --scan-cut=<id>      Number of the "head cut" point (0-based).
                                Default is 0, i.e. the first superblock found.
                                Use -Sc -1 to disable head cutting completely.
