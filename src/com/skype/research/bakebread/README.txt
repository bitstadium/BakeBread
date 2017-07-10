USAGE

 java -jar bakebread.jar [OPTION]... FILENAME.dmp

EXAMPLE

 java -jar bakebread.jar -Dc -CS ./out -CC core.dump
        -P app/lib/unstripped:device/sysroot -Vdh -Mea -Or mini.dmp

 Open mini.dmp, display crash information (thread and signal), expand raw dump
 sections into the "out" subfolder and convert the minidump into ELF core dump
 navigable with GDB with a courtesy debugging session loading script (*.gdb).
 
 Use the provided path to search for app and OS binaries. Validate whether the
 dump is consistent and host libraries match it. Assume ELF and ARM. Output all
 "readable" data, that is, contents of the dump itself and matching host files,
 into the eventual core dump.

LIMITATIONS

 At the moment, only 32-bit ARM/Linux (Android) target is supported.
 (The utility itself is, of course, runnable anywhere.)

OPTIONS

 Short options with a parameter use ' ' delimiter, long options use '='.
 Example: "-CC core.dump" and "--convert-core=core.dump" are equivalent.
 
 Multiple options of the same type are allowed and naturally combined.

Display options:
 -Ds, --display=stats       Show file statistic (size, stream count, etc.)
 -Dr, --display=roots       Root stream list (standalone top-level streams)
 -Dc, --display=crashed     Signal information and crashed thread context
 -Dt, --display=threads     All thread contexts (status and registers)
 -Dm, --display=mapping     Memory mapping information in /proc/PID/maps format
 -Dv, --display=verbose     Display all sections listed above (DEFAULT)
 -DD, --display=debug       Display miscellaneous debugging information
 
 (Output options can be combined, as in "-Dst" or "--display=streams,threads".)

Converter options:
 -CS <DIR>, --convert-split=<DIR>   Split the dump into individual streams.
 -CC <FILE>, --convert-core=<FILE>  Produce a core dump file, along with
                                    - a GDB symbol load script in <FILE>.gdb
                                    - a host/dump delta report in <FILE>.diff
                                        (see Soft Differencing section below)
  
Reference input options:
 -P <PATH>, --path=PATH     Binary and symbol file path.
                            Default separator is implied (*nix ':', win ';')
                            Path entries are tried against qualified paths,
                            then base names, e.g. in the following case
                            
                                module name = /system/lib/libc.so 
                                path entry  = /home/lxe/Devices/Nexus5
                                
                            the following host file paths are tried:
                             
                                /home/lxe/Devices/Nexus5/system/lib/libc.so 
                                /home/lxe/Devices/Nexus5/libc.so

Input integrity validation options:
 -Vd, --validate=dump       Compare overlapping memory streams in the dump.
                                Failures indicate a likely corrupt dump.
 -Vh, --validate=host       Compare dumped file mappings with host file data.
                                Failures indicate wrong or outdated firmware,
                                    application or application version.
 -Vl, --validate=relax      Compute proximity/similarity heuristics but never
                                fail however poor the correlation is. Display
                                    a side by side diff in the worst case.
                                (Note: this is a better way to go than setting 
                                    all thresholds to maximum tolerance.)
 -Vs, --validate=strict     Disable proximity/similarity heuristics and only
                                compare memory areas bit-exact. Fail as soon
                                    as a single dissimilarity is noticed.
                                        Overrides (disables) -Vl.

Soft Differencing options (take effect unless strict validation is turned on):
 -So, --soft-max-outlier    Percentage of stitches with a unique translation
                            value, i.e. not reliably representing a shifted,
                            copied or moved data range.
                                The default is 0.5, or 50%
                                
 -Sg, --soft-min-growing    Percentage of stitches forming a longest growing
                            subsequence. The higher the value is, the better
                            the modified subsequence "co-evolves" with the
                            original one.
                                The default is 0.4, or 40%
                                
 -Sh, --soft-max-hamming    Percentage of different bytes, adjusted for skew
                            and range relocation.
                                The default is 0.9, or 90%
                                
 -Sb, --soft-bit-hamming    Percentage of different *bits*, adjusted for skew
                            and range relocation.
                                The default is 0.3, or 30%.
 
 Algorithm selection is not yet exposed as there is only one path implemented.
 The current implementation uses a Tamien rolling hash to build a metric tree,
 which is then searched for closest matches ("stitches"). Adjacent "stitches"
 with a similar translation ("drift") are grouped into ranges. Remaining gaps
 are "healed" in such a way as to produce a smallest total Hamming.
 Other notable algorithms employed are the famous O(N) median computation and
 the longest monotonic (increasing) subsequence with O(N logN) complexity.

Memory mapping recovery options (at least one, if /proc/<PID/maps are missing):
 -Me, --modules=elf         Use ELF section information to infer mappings
                                if /proc/<PID>/maps are unavailable.
                            During core dump generation, write a courtesy
                                GDB script that would load the module symbols
                                according to their section locations.
 -Mr, --modules=raw         Do not use ELF section information. In this case,
                                one module entry produces exactly one mapping,
                                which may deviate from the linker behavior.
                                Suppresses certain validation and heuristics.
 -Ma, --modules=arm         "Bleach" BL (same-module procedure call) offsets
                                from code being compared. This is a cheap hack
                                to improve comparison reliability by excluding
                                the "linkage-sensitive" parts of method body.
                                All words that look like BL are amended, i.e.
                                no sophisticated code analysis is performed.

Mark words to fill unavailable or unreliable memory:
 -Fr, --fill-run-time       areas not mapped to any file (e.g. heap)
                                the default value is { ea } for "hEAp"
 -Fn, --fill-not-found      mapped to a file not found in the provided path
                                the default value is { 04 } for "404"
 -Fe, --fill-file-end       mapped to a file but extending beyond its end
                                the default value is { e0 ff } "EofF"
 -Fu, --fill-unreliable     writable data (possibly modified after reading)
 -Fp, --fill-private-pg     unreadable private page, can't have been dumped
  
 Hexadecimal format of the "fill word" is always implied, regardless of "0x".
 The pattern repetition rules are as follows:
 - if a value fits in a byte, it fills every byte;
 - otherwise, if a value fits in a [16-bit] halfword, it fills every halfword,
 - otherwise, the value is trimmed to 32 bits and fills every [32-byte] word.
  
 Examples:
  -F?=0xff       => ff ff ff ff ff ff ff ff
  -F?=0xe0ff     => e0 ff e0 ff e0 ff e0 ff
  -F?=0xdeadf00d => de ad f0 0d de ad f0 0d

Core dump memory output options:
 -Om, --out=mdp             Only write contents of the original minidump.
 -Or, --out=ro              [*] Write contents of the original minidump with
                                readonly non-relocatable host file sections.
 -Ow, --out=rw              Write all areas but memory fog (minidump streams,
                                readonly host file sections and even initial
                                static memory values, unless filled over.)
 -Oa, --out=all             Write all mappings, fill unknown memory contents
                                with "memory fog" (see -F option).
                                
 [*] This is the default value.

LEGEND: *.diff files

*.diff is generated by the core dump generator dump vs. host validation step
if deviations are encountered but aren't considered critical. To some extent,
it mimics the well-known hexadecimal dump and textual patch formats, but, at
the same time, adds notable extensions to both. Consider the example:

Mapping: 400f2000-40138000 r-xp 00000000 b3:0f 1413       /system/lib/libc.so
Quality: 2 (Zero skew near start and end, but varies in between.)
===================================================================
--- file:libc.so@0+46000
+++ file:<open-file>@406b0+2000
@@ -00017000,6766 +40109000,6766 ^1'3" @@
-000189e0|03 00 12 e3 23 00 00 1a 03 20 10 e2 03 00 c0 e3|....#.... ......|
+4010a9e0|03 00 12 e3 24 00 00 1a 03 20 10 e2 03 00 c0 e3|....$.... ......|
^        |            07                                 |    3           |
@@ +4010aa6e,4 @@
+4010aa60|                                          a0 e1|              ..|
+4010aa70|03 00                                          |..              |
@@ -00018a6e,422 +4010aa72,422 :+4 ^1'3" @@
-00018a6c|      63 e0 04 40 9d e4 1e ff 2f e1 03 00 10 e3|  c..@..../.....|
+4010aa70|      40 e0 04 40 9d e4 1e ff 2f e1 03 00 10 e3|  @..@..../.....|
^        |      23                                       |  3             |
@@ -00018c14,4 @@
-00018c10|            00 00 00 00                        |    ....        |

"Mapping" is the memory mapping, as in /proc/*/maps, that the dumped memory
    area belongs to. The host file is considered "original" (---) while the
    dumped memory area "modified" (+++).
"Quality" varies from 1 "All changes in-place" to 5 "Insertions or deletions
    cause skew to accumulate". 0 is no change, not worth reporting; 6 stands
    for "cannot correlate contents at all" and fails the conversion.
       
You may want to know how to force dump generation even if one specific local
file is not an accurate representation of a target file. One way to go is to
change the -S* sensitivity settings. Another way is not to validate the dump
against the host (unset -Vh). A third way would be to remove a specific file
from the -P path (e.g. temporarily rename *.so to *.so.skipme or something.)

@@ -00018a6e,422 +4010aa72,422 :+4 ^1'3" @@
    Addresses in the local file are file offsets, while addresses in the dump
    are application memory addresses at crash time. -Addr,N and +Addr,N stand
    for deletion and insertion of N bytes, respectively. A negative number of
    bytes deleted (-Addr,-N) indicates a "rewind", i.e. a replayed, or nearly
    replayed, "original" byte sequence.
    
    :+Drift and :-Drift indicate a deviation between "original" and "modified"
    chunk placement. A change of zero drift does not affect validity of symbol
    data, so it's more or less harmless (unless your further analysis depends
    on accuracy of the disassembly, down to usage of specific registers) while
    a nonzero drift advises taking addr2line output with criticism.
    :? indicates that a chunk cannot be matched to its in-sequence counterpart
    but "reminds" a distant, out-of-sequence, piece of the original file. Such
    fragments are displayed **in addition** to the delete/insert pair they are
    embedded in.
    
    ^bytes'bits" are numbers of different bytes and bits when the chunks have
    been placed side by side. They are computed by XORing the values after the
    skew has been ruled out, hence the ^ sign. Note that the "bytes" and "bits"
    are incomparable totals; they don't add up.
    
-00018a6c|      63 e0 04 40 9d e4 1e ff 2f e1 03 00 10 e3|  c..@..../.....|
+4010aa70|      40 e0 04 40 9d e4 1e ff 2f e1 03 00 10 e3|  @..@..../.....|
^        |      23                                       |  3             |
    When two chunks are considered a match and compared bytewise side-by-side,
    the first two lines represent the original and modified byte values in the
    conventional "hex+ascii" format. The third line displays the bitwise XOR
    in the hexadecimal part and **the count of deviating bits** in the "ascii"
    part. In the example above, 0x23 is the bitwise XOR between original 0x63
    and modified 0x40, and "3" on the right side is the bit count of 0x23.

Note the following "change" is harmless:

 ===================================================================
 --- file:libSomeLib.so@0+1c59000
 +++ file:<open-file>@ac130+1000
 @@ -00000000,4096 +9fba3000,4096 ^5'21" @@
 -00000020|ac f7 7d 0f 00 00 00 05 34 00 20 00 08 00 28 00|..}.....4. ...(.|
 +9fba3020|ac 05 d6 01 00 00 00 05 34 00 20 00 08 00 28 00|........4. ...(.|
 ^        |   f2 ab 0e                                    | 553            |
 -00000030|26 00 25 00 06 00 00 00 34 00 00 00 34 00 00 00|&.%.....4...4...|
 +9fba3030|18 00 17 00 06 00 00 00 34 00 00 00 34 00 00 00|........4...4...|
 ^        |3e    32                                       |5 3             |
 
The above offsets (0x20, 0x30 and 0x32) are within the ELF file header, so the
differences owe to the different section count and section table offset in the
stripped and the debug-friendly versions of the binary, respectively.

N.B. Be aware that the differencing engine will fail spectacularly on strings.
It is precisely aimed at matching sequences of 16-bit half-words of executable
code and aligned data.

AMENITIES

"Amenities" are extra features available with a numeric switch.
    The switch must be the first command line argument.
    
 0  Expand a "sparse" firmware partition image into a mountable bit-exact disk
    image. This is normally needed to extract the contents of /system/(lib|bin)
    from a firmware deliverable. Usage:
        
        java -jar bakebread.jar -0 sparse-image.img mountable-image.img
            # expand a single-file image

        java -jar bakebread.jar -0 [image.sparsechunk.N]... mountable-image.img
            # expand a multi-chunk image

        java -jar bakebread.jar -0
            # display a help page

