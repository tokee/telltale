telltale
========

Detection of resized images, uncalibrated scanners and other problems with digitized images.


Up-scaled images
================

Up-scaled images is defined here as bitmaps that at some point has been resized to a dimension larger
than the original. This is sometimes done behind the scenes by scanner software, sometimes explicitly
by operators caring more about the bottom line than delivering the best product to customers.

ResizeDetect is currently detecting simple x2 up-scales, with the typical case being 200DPI scans
resized to 400DPI. The detection is extremely reliable for the trivial Nearest-Neighbour scaling and
fairly reliable for a real-world sample from a scanner which used what looks like a mix between
Nearest-Neighbour and Bi-linear interpolation (sample hvide_19681224_1.png in the test resources).

The working hypothesis is that the alleged up-scaled image is made up of 2x2 pixel super-pixels where
the colors (or greyscales) of the pixels within a super-pixel are fairly uniform. The trivial example
is again Nearest-Neighbour, where all the pixels within a super-pixel are identical.

Consider the 4x4 pixel image

    RrGg
    rRgG
    BbTt
    BbtT

Where each letter represents a color and `R` is close to `r` etc. As can be seen, the image is indeed
made up of 2x2 super-pixels. The problem is that the image can be cropped other otherwise manipulated
so that the super-pixels are offset by 1 pixel horizontally, vertically or both. So we need to check
offsets (0, 0), (0, 1), (1, 0) and (1,1).

For each offset, four hypothesises are stated:
1. The super-pixel is made up of uniform pixels: Offset (0, 0) in the sample.
2. The super-pixel is made up of two pairs of pixels, with the two top pixels being one pair and the
   two bottom pixels being the second pair: Offset (0, 1) in the sample.
3. The super-pixel is made up of two pairs of pixels, with the two left pixels being one pair and the
   two right pixels being the second pair: Offset (1, 0) in the sample.
4. The super-pixel is made up of non-uniform pixels: Offset (1, 0) in the sample.


- Toke Eskildsen, te@statsbiblioteket.dk (work) / te@ekot.dk (private)
