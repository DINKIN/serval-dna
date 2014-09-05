/*
Serval DNA Universally Unique Identifier support
Copyright (C) 2013 Serval Project Inc.
 
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
 
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
 
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

#ifndef __SERVAL_DNA__UUID_H
#define __SERVAL_DNA__UUID_H

#include <stdint.h>
#include <alloca.h>
#include <string.h>
#include "strbuf.h"

#ifndef __SERVAL_DNA__UUID_H_INLINE
# if __GNUC__ && !__GNUC_STDC_INLINE__
#  define __SERVAL_DNA__UUID_H_INLINE extern inline
# else
#  define __SERVAL_DNA__UUID_H_INLINE inline
# endif
#endif

/* A UUID is defined by RFC-4122 as a 128-bit identifier with the two most
 * significant bits of the ninth byte being 1 and 0, which indicates the
 * "variant" that is described by the RFC.  Other variants exist, but are not
 * supported here, and are treated as INVALID by the functions defined below.
 * Any attempt to pass an invalid UUID to a function that requires a valid UUID
 * as input will probably result in the calling process being aborted (see
 * SIGABRT, abort(3)).
 *
 * In a valid UUID, the four lowest significant bits of the seventh byte define
 * the "version" of the UUID, which essentially indicates how it was generated.
 * The RFC defines five SUPPORTED versions.  Any other version is UNSUPPORTED.
 *
 * The fields in the UUID 'record' structure are stored in network byte order,
 * so code wishing to make use of the record structure must use ntohl(3) and
 * ntohs(3) to read values, and htonl(3) and htons(3) to assign values.
 *
 * OS-X already defines a "uuid_t" typedef, so we use a different symbol.
 *
 * @author Andrew Bettison <andrew@servalproject.com>
 */
typedef struct serval_uuid {
  union {
    struct {
      uint32_t time_low;
      uint16_t time_mid;
      uint16_t time_hi_and_version;
      uint8_t clock_seq_hi_and_reserved;
      uint8_t clock_seq_low;
      unsigned char node[6]; // uint48_t
    } record;
    unsigned char binary[16];
  } u;
} serval_uuid_t;

enum uuid_version {
  UUID_VERSION_UNSUPPORTED = 0,
  UUID_VERSION_TIME_BASED = 1,
  UUID_VERSION_DCE_SECURITY = 2,
  UUID_VERSION_NAME_MD5 = 3,
  UUID_VERSION_RANDOM = 4,
  UUID_VERSION_NAME_SHA1 = 5
};

__SERVAL_DNA__UUID_H_INLINE int cmp_uuid_t(const serval_uuid_t *a, const serval_uuid_t *b) {
  return memcmp(a->u.binary, b->u.binary, sizeof a->u.binary);
}

__SERVAL_DNA__UUID_H_INLINE int uuid_is_valid(const serval_uuid_t *any_uuid) {
  return (any_uuid->u.record.clock_seq_hi_and_reserved & 0xc0) == 0x80;
}

enum uuid_version uuid_get_version(const serval_uuid_t *valid_uuid);
void uuid_set_version(serval_uuid_t *valid_uuid, enum uuid_version);

/* Returns -1 if error (eg, cannot open /dev/urandom), 0 if successful.
 */
int uuid_generate_random(serval_uuid_t *dest_uuid);

/* Formats the given valid UUID in its canonical string representation:
 *    XXXXXXXX-VXXX-MXXX-XXXXXXXXXXXX
 * where X is any hex digit
 *       V is 1, 2, 3, 4 or 5 (supported versions) or any other hex digit
 *            (unsupported versions)
 *       M is 8, 9, A or B (high two bits are variant 01)
 *
 * The 'dst' argument must point to a buffer of 37 bytes.  The first 36 bytes
 * are filled with the representation shown above, and the 37th byte dst[36] is
 * set to NUL '\0'.
 *
 * Returns the 'dst' argument.
 *
 * @author Andrew Bettison <andrew@servalproject.com>
 */
char *uuid_to_str(const serval_uuid_t *valid_uuid, char *dst);
#define UUID_STRLEN 36
#define alloca_uuid_str(uuid) uuid_to_str(&(uuid), alloca(UUID_STRLEN + 1))

/* Append a UUID to the given strbuf, formatted as per the uuid_to_str() function.
 */
strbuf strbuf_uuid(strbuf, const serval_uuid_t *valid_uuid);

/* Parse a canonical UUID string (as generated by uuid_to_str()) into a valid
 * UUID, which may or not be supported.
 *
 * Returns 1 if a valid UUID is parsed, storing the value in *result (unless result is NULL) and
 * storing a pointer to the immediately succeeding character in *afterp.  If afterp is NULL then
 * returns 0 unless the immediately succeeding character is a NUL '\0'.  If no UUID is parsed or
 * if the UUID is not valid, then returns 0, leaving *result unchanged and
 * setting setting *afterp to point to the character where parsing failed.
 *
 * @author Andrew Bettison <andrew@servalproject.com>
 */
int str_to_uuid(const char *str, serval_uuid_t *result, const char **afterp);

#endif //__SERVAL_DNA__OS_H
