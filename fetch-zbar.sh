#!/bin/sh
echo "fetching zbar sources"
hg clone http://zbar.hg.sourceforge.net:8000/hgroot/zbar/zbar zbar

cd jni

echo "creating symlinks"
ln -s ../zbar/zbar/config.c config.c
ln -s ../zbar/zbar/convert.c convert.c
ln -s ../zbar/zbar/debug.h debug.h
ln -s ../zbar/zbar/decoder decoder
ln -s ../zbar/zbar/decoder.c decoder.c
ln -s ../zbar/zbar/decoder.h decoder.h
ln -s ../zbar/zbar/error.c error.c
ln -s ../zbar/zbar/error.h error.h
ln -s ../zbar/zbar/event.h event.h
ln -s ../zbar/zbar/image.c image.c
ln -s ../zbar/zbar/image.h image.h
ln -s ../zbar/zbar/img_scanner.c img_scanner.c
ln -s ../zbar/zbar/img_scanner.h img_scanner.h
ln -s ../zbar/zbar/jpeg.c jpeg.c
ln -s ../zbar/zbar/mutex.h mutex.h
ln -s ../zbar/zbar/processor processor
ln -s ../zbar/zbar/processor.c processor.c
ln -s ../zbar/zbar/processor.h processor.h
ln -s ../zbar/zbar/qrcode qrcode
ln -s ../zbar/zbar/qrcode.h qrcode.h
ln -s ../zbar/zbar/refcnt.c refcnt.c
ln -s ../zbar/zbar/refcnt.h refcnt.h
ln -s ../zbar/zbar/scanner.c scanner.c
ln -s ../zbar/zbar/svg.c svg.c
ln -s ../zbar/zbar/svg.h svg.h
ln -s ../zbar/zbar/symbol.c symbol.c
ln -s ../zbar/zbar/symbol.h symbol.h
ln -s ../zbar/zbar/thread.h thread.h
ln -s ../zbar/zbar/timer.h timer.h
ln -s ../zbar/zbar/video video
ln -s ../zbar/zbar/video.c video.c
ln -s ../zbar/zbar/video.h video.h
ln -s ../zbar/zbar/window window
ln -s ../zbar/zbar/window.c window.c
ln -s ../zbar/zbar/window.h window.h

ln -s ../zbar/include/zbar.h zbar.h

echo "done."

