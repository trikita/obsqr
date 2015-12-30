#include <stdlib.h>
#include <iconv.h>

#include <android/log.h>

iconv_t iconv_open (const char *tocode, const char *fromcode) {
	return NULL;
}

size_t iconv (iconv_t cd, char **inbuf, size_t *inbytesleft, 
		char **outbuf, size_t *outbytesleft) {
	size_t sz = *inbytesleft;
	memset(*outbuf, 0, *outbytesleft);
	memcpy(*outbuf, *inbuf, sz);

	*inbuf += sz;
	*outbuf += sz;
	*inbytesleft -= sz;
	*outbytesleft -= sz;
	return sz;
}

int iconv_close (iconv_t cd) {
	return 0;
}

