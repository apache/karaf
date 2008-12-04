package aQute.lib.osgi;

public class OpCodes {
	final static short	nop				= 0x00;			// [No change] performs
														// no
	// operation
	final static short	aconst_null		= 0x01;			// ? null pushes a null
	// reference onto the stack
	final static short	iconst_m1		= 0x02;			// ? -1 loads the int
														// value -1
	// onto the stack
	final static short	iconst_0		= 0x03;			// ? 0 loads the int
														// value 0
	// onto the stack
	final static short	iconst_1		= 0x04;			// ? 1 loads the int
														// value 1
	// onto the stack
	final static short	iconst_2		= 0x05;			// ? 2 loads the int
														// value 2
	// onto the stack
	final static short	iconst_3		= 0x06;			// ? 3 loads the int
														// value 3
	// onto the stack
	final static short	iconst_4		= 0x07;			// ? 4 loads the int
														// value 4
	// onto the stack
	final static short	iconst_5		= 0x08;			// ? 5 loads the int
														// value 5
	// onto the stack
	final static short	lconst_0		= 0x09;			// ? 0L pushes the long
														// 0 onto
	// the stack
	final static short	bipush			= 0x10;			// byte ? value pushes a
														// byte
	// onto the stack as an integer
	// value
	final static short	sipush			= 0x11;			// byte1, byte2 ? value
														// pushes a
	// signed integer (byte1 << 8 +
	// byte2) onto the stack
	final static short	ldc				= 0x12;			// index ? value pushes
														// a
	// constant #index from a
	// constant pool (String, int,
	// float or class type) onto the
	// stack
	final static short	ldc_w			= 0x13;			// indexbyte1,
														// indexbyte2 ?
	// value pushes a constant
	// #index from a constant pool
	// (String, int, float or class
	// type) onto the stack (wide
	// index is constructed as
	// indexbyte1 << 8 + indexbyte2)
	final static short	ldc2_w			= 0x14;			// indexbyte1,
														// indexbyte2 ?
	// value pushes a constant
	// #index from a constant pool
	// (double or long) onto the
	// stack (wide index is
	// constructed as indexbyte1 <<
	// 8 + indexbyte2)
	final static short	iload			= 0x15;			// index ? value loads
														// an int
	// value from a variable #index
	final static short	lload			= 0x16;			// index ? value load a
														// long
	// value from a local variable
	// #index
	final static short	fload			= 0x17;			// index ? value loads a
														// float
	// value from a local variable
	// #index
	final static short	dload			= 0x18;			// index ? value loads a
														// double
	// value from a local variable
	// #index
	final static short	aload			= 0x19;			// index ? objectref
														// loads a
	// reference onto the stack from
	// a local variable #index
	final static short	lload_2			= 0x20;			// ? value load a long
														// value
	// from a local variable 2
	final static short	lload_3			= 0x21;			// ? value load a long
														// value
	// from a local variable 3
	final static short	fload_0			= 0x22;			// ? value loads a float
														// value
	// from local variable 0
	final static short	fload_1			= 0x23;			// ? value loads a float
														// value
	// from local variable 1
	final static short	fload_2			= 0x24;			// ? value loads a float
														// value
	// from local variable 2
	final static short	fload_3			= 0x25;			// ? value loads a float
														// value
	// from local variable 3
	final static short	dload_0			= 0x26;			// ? value loads a
														// double from
	// local variable 0
	final static short	dload_1			= 0x27;			// ? value loads a
														// double from
	// local variable 1
	final static short	dload_2			= 0x28;			// ? value loads a
														// double from
	// local variable 2
	final static short	dload_3			= 0x29;			// ? value loads a
														// double from
	// local variable 3
	final static short	faload			= 0x30;			// arrayref, index ?
														// value loads
	// a float from an array
	final static short	daload			= 0x31;			// arrayref, index ?
														// value loads
	// a double from an array
	final static short	aaload			= 0x32;			// arrayref, index ?
														// value loads
	// onto the stack a reference
	// from an array
	final static short	baload			= 0x33;			// arrayref, index ?
														// value loads
	// a byte or Boolean value from
	// an array
	final static short	caload			= 0x34;			// arrayref, index ?
														// value loads
	// a char from an array
	final static short	saload			= 0x35;			// arrayref, index ?
														// value load
	// short from array
	final static short	istore			= 0x36;			// index value ? store
														// int value
	// into variable #index
	final static short	lstore			= 0x37;			// index value ? store a
														// long
	// value in a local variable
	// #index
	final static short	fstore			= 0x38;			// index value ? stores
														// a float
	// value into a local variable
	// #index
	final static short	dstore			= 0x39;			// index value ? stores
														// a double
	// value into a local variable
	// #index
	final static short	lstore_1		= 0x40;			// value ? store a long
														// value in
	// a local variable 1
	final static short	lstore_2		= 0x41;			// value ? store a long
														// value in
	// a local variable 2
	final static short	lstore_3		= 0x42;			// value ? store a long
														// value in
	// a local variable 3
	final static short	fstore_0		= 0x43;			// value ? stores a
														// float value
	// into local variable 0
	final static short	fstore_1		= 0x44;			// value ? stores a
														// float value
	// into local variable 1
	final static short	fstore_2		= 0x45;			// value ? stores a
														// float value
	// into local variable 2
	final static short	fstore_3		= 0x46;			// value ? stores a
														// float value
	// into local variable 3
	final static short	dstore_0		= 0x47;			// value ? stores a
														// double into
	// local variable 0
	final static short	dstore_1		= 0x48;			// value ? stores a
														// double into
	// local variable 1
	final static short	dstore_2		= 0x49;			// value ? stores a
														// double into
	// local variable 2
	final static short	lastore			= 0x50;			// arrayref, index,
														// value ?
	// store a long to an array
	final static short	fastore			= 0x51;			// arreyref, index,
														// value ?
	// stores a float in an array
	final static short	dastore			= 0x52;			// arrayref, index,
														// value ?
	// stores a double into an array
	final static short	aastore			= 0x53;			// arrayref, index,
														// value ?
	// stores into a reference to an
	// array
	final static short	bastore			= 0x54;			// arrayref, index,
														// value ?
	// stores a byte or Boolean
	// value into an array
	final static short	castore			= 0x55;			// arrayref, index,
														// value ?
	// stores a char into an array
	final static short	sastore			= 0x56;			// arrayref, index,
														// value ?
	// store short to array
	final static short	pop				= 0x57;			// value ? discards the
														// top
	// value on the stack
	final static short	pop2			= 0x58;			// {value2, value1} ?
														// discards
	// the top two values on the
	// stack (or one value, if it is
	// a double or long)
	final static short	dup				= 0x59;			// value ? value, value
	// duplicates the value on top
	// of the stack
	final static short	iadd			= 0x60;			// value1, value2 ?
														// result adds
	// two ints together
	final static short	ladd			= 0x61;			// value1, value2 ?
														// result add
	// two longs
	final static short	fadd			= 0x62;			// value1, value2 ?
														// result adds
	// two floats
	final static short	dadd			= 0x63;			// value1, value2 ?
														// result adds
	// two doubles
	final static short	isub			= 0x64;			// value1, value2 ?
														// result int
	// subtract
	final static short	lsub			= 0x65;			// value1, value2 ?
														// result
	// subtract two longs
	final static short	fsub			= 0x66;			// value1, value2 ?
														// result
	// subtracts two floats
	final static short	dsub			= 0x67;			// value1, value2 ?
														// result
	// subtracts a double from
	// another
	final static short	imul			= 0x68;			// value1, value2 ?
														// result
	// multiply two integers
	final static short	lmul			= 0x69;			// value1, value2 ?
														// result
	// multiplies two longs
	final static short	irem			= 0x70;			// value1, value2 ?
														// result
	// logical int remainder
	final static short	lrem			= 0x71;			// value1, value2 ?
														// result
	// remainder of division of two
	// longs
	final static short	frem			= 0x72;			// value1, value2 ?
														// result gets
	// the remainder from a division
	// between two floats
	final static short	drem			= 0x73;			// value1, value2 ?
														// result gets
	// the remainder from a division
	// between two doubles
	final static short	ineg			= 0x74;			// value ? result negate
														// int
	final static short	lneg			= 0x75;			// value ? result
														// negates a long
	final static short	fneg			= 0x76;			// value ? result
														// negates a
	// float
	final static short	dneg			= 0x77;			// value ? result
														// negates a
	// double
	final static short	ishl			= 0x78;			// value1, value2 ?
														// result int
	// shift left
	final static short	lshl			= 0x79;			// value1, value2 ?
														// result
	// bitwise shift left of a long
	// value1 by value2 positions
	final static short	ior				= 0x80;			// value1, value2 ?
														// result
	// logical int or
	final static short	lor				= 0x81;			// value1, value2 ?
														// result
	// bitwise or of two longs
	final static short	ixor			= 0x82;			// value1, value2 ?
														// result int
	// xor
	final static short	lxor			= 0x83;			// value1, value2 ?
														// result
	// bitwise exclusive or of two
	// longs
	final static short	iinc			= 0x84;			// index, const [No
														// change]
	// increment local variable
	// #index by signed byte const
	final static short	i2l				= 0x85;			// value ? result
														// converts an
	// int into a long
	final static short	i2f				= 0x86;			// value ? result
														// converts an
	// int into a float
	final static short	i2d				= 0x87;			// value ? result
														// converts an
	// int into a double
	final static short	l2i				= 0x88;			// value ? result
														// converts a
	// long to an int
	final static short	l2f				= 0x89;			// value ? result
														// converts a
	// long to a float
	final static short	d2f				= 0x90;			// value ? result
														// converts a
	// double to a float
	final static short	i2b				= 0x91;			// value ? result
														// converts an
	// int into a byte
	final static short	i2c				= 0x92;			// value ? result
														// converts an
	// int into a character
	final static short	i2s				= 0x93;			// value ? result
														// converts an
	// int into a short
	final static short	lcmp			= 0x94;			// value1, value2 ?
														// result
	// compares two longs values
	final static short	fcmpl			= 0x95;			// value1, value2 ?
														// result
	// compares two floats
	final static short	fcmpg			= 0x96;			// value1, value2 ?
														// result
	// compares two floats
	final static short	dcmpl			= 0x97;			// value1, value2 ?
														// result
	// compares two doubles
	final static short	dcmpg			= 0x98;			// value1, value2 ?
														// result
	// compares two doubles
	final static short	ifeq			= 0x99;			// branchbyte1,
														// branchbyte2
	// value ? if value is 0, branch
	// to instruction at
	// branchoffset (signed short
	// constructed from unsigned
	// bytes branchbyte1 << 8 +
	// branchbyte2)
	final static short	lconst_1		= 0x0a;			// ? 1L pushes the long
														// 1 onto
	// the stack
	final static short	fconst_0		= 0x0b;			// ? 0.0f pushes 0.0f on
														// the
	// stack
	final static short	fconst_1		= 0x0c;			// ? 1.0f pushes 1.0f on
														// the
	// stack
	final static short	fconst_2		= 0x0d;			// ? 2.0f pushes 2.0f on
														// the
	// stack
	final static short	dconst_0		= 0x0e;			// ? 0.0 pushes the
														// constant 0.0
	// onto the stack
	final static short	dconst_1		= 0x0f;			// ? 1.0 pushes the
														// constant 1.0
	// onto the stack
	final static short	iload_0			= 0x1a;			// ? value loads an int
														// value
	// from variable 0
	final static short	iload_1			= 0x1b;			// ? value loads an int
														// value
	// from variable 1
	final static short	iload_2			= 0x1c;			// ? value loads an int
														// value
	// from variable 2
	final static short	iload_3			= 0x1d;			// ? value loads an int
														// value
	// from variable 3
	final static short	lload_0			= 0x1e;			// ? value load a long
														// value
	// from a local variable 0
	final static short	lload_1			= 0x1f;			// ? value load a long
														// value
	// from a local variable 1
	final static short	aload_0			= 0x2a;			// ? objectref loads a
														// reference
	// onto the stack from local
	// variable 0
	final static short	aload_1			= 0x2b;			// ? objectref loads a
														// reference
	// onto the stack from local
	// variable 1
	final static short	aload_2			= 0x2c;			// ? objectref loads a
														// reference
	// onto the stack from local
	// variable 2
	final static short	aload_3			= 0x2d;			// ? objectref loads a
														// reference
	// onto the stack from local
	// variable 3
	final static short	iaload			= 0x2e;			// arrayref, index ?
														// value loads
	// an int from an array
	final static short	laload			= 0x2f;			// arrayref, index ?
														// value load
	// a long from an array
	final static short	astore			= 0x3a;			// index objectref ?
														// stores a
	// reference into a local
	// variable #index
	final static short	istore_0		= 0x3b;			// value ? store int
														// value into
	// variable 0
	final static short	istore_1		= 0x3c;			// value ? store int
														// value into
	// variable 1
	final static short	istore_2		= 0x3d;			// value ? store int
														// value into
	// variable 2
	final static short	istore_3		= 0x3e;			// value ? store int
														// value into
	// variable 3
	final static short	lstore_0		= 0x3f;			// value ? store a long
														// value in
	// a local variable 0
	final static short	dstore_3		= 0x4a;			// value ? stores a
														// double into
	// local variable 3
	final static short	astore_0		= 0x4b;			// objectref ? stores a
	// reference into local variable
	// 0
	final static short	astore_1		= 0x4c;			// objectref ? stores a
	// reference into local variable
	// 1
	final static short	astore_2		= 0x4d;			// objectref ? stores a
	// reference into local variable
	// 2
	final static short	astore_3		= 0x4e;			// objectref ? stores a
	// reference into local variable
	// 3
	final static short	iastore			= 0x4f;			// arrayref, index,
														// value ?
	// stores an int into an array
	final static short	dup_x1			= 0x5a;			// value2, value1 ?
														// value1,
	// value2, value1 inserts a copy
	// of the top value into the
	// stack two values from the top
	final static short	dup_x2			= 0x5b;			// value3, value2,
														// value1 ?
	// value1, value3, value2,
	// value1 inserts a copy of the
	// top value into the stack two
	// (if value2 is double or long
	// it takes up the entry of
	// value3, too) or three values
	// (if value2 is neither double
	// nor long) from the top
	final static short	dup2			= 0x5c;			// {value2, value1} ?
														// {value2,
	// value1}, {value2, value1}
	// duplicate top two stack words
	// (two values, if value1 is not
	// double nor long; a single
	// value, if value1 is double or
	// long)
	final static short	dup2_x1			= 0x5d;			// value3, {value2,
														// value1} ?
	// {value2, value1}, value3,
	// {value2, value1} duplicate
	// two words and insert beneath
	// third word (see explanation
	// above)
	final static short	dup2_x2			= 0x5e;			// {value4, value3},
														// {value2,
	// value1} ? {value2, value1},
	// {value4, value3}, {value2,
	// value1} duplicate two words
	// and insert beneath fourth
	// word
	final static short	swap			= 0x5f;			// value2, value1 ?
														// value1,
	// value2 swaps two top words on
	// the stack (note that value1
	// and value2 must not be double
	// or long)
	final static short	fmul			= 0x6a;			// value1, value2 ?
														// result
	// multiplies two floats
	final static short	dmul			= 0x6b;			// value1, value2 ?
														// result
	// multiplies two doubles
	final static short	idiv			= 0x6c;			// value1, value2 ?
														// result
	// divides two integers
	final static short	ldiv			= 0x6d;			// value1, value2 ?
														// result
	// divide two longs
	final static short	fdiv			= 0x6e;			// value1, value2 ?
														// result
	// divides two floats
	final static short	ddiv			= 0x6f;			// value1, value2 ?
														// result
	// divides two doubles
	final static short	ishr			= 0x7a;			// value1, value2 ?
														// result int
	// shift right
	final static short	lshr			= 0x7b;			// value1, value2 ?
														// result
	// bitwise shift right of a long
	// value1 by value2 positions
	final static short	iushr			= 0x7c;			// value1, value2 ?
														// result int
	// shift right
	final static short	lushr			= 0x7d;			// value1, value2 ?
														// result
	// bitwise shift right of a long
	// value1 by value2 positions,
	// unsigned
	final static short	iand			= 0x7e;			// value1, value2 ?
														// result
	// performs a logical and on two
	// integers
	final static short	land			= 0x7f;			// value1, value2 ?
														// result
	// bitwise and of two longs
	final static short	l2d				= 0x8a;			// value ? result
														// converts a
	// long to a double
	final static short	f2i				= 0x8b;			// value ? result
														// converts a
	// float to an int
	final static short	f2l				= 0x8c;			// value ? result
														// converts a
	// float to a long
	final static short	f2d				= 0x8d;			// value ? result
														// converts a
	// float to a double
	final static short	d2i				= 0x8e;			// value ? result
														// converts a
	// double to an int
	final static short	d2l				= 0x8f;			// value ? result
														// converts a
	// double to a long
	final static short	ifne			= 0x9a;			// branchbyte1,
														// branchbyte2
	// value ? if value is not 0,
	// branch to instruction at
	// branchoffset (signed short
	// constructed from unsigned
	// bytes branchbyte1 << 8 +
	// branchbyte2)
	final static short	iflt			= 0x9b;			// branchbyte1,
														// branchbyte2
	// value ? if value is less than
	// 0, branch to instruction at
	// branchoffset (signed short
	// constructed from unsigned
	// bytes branchbyte1 << 8 +
	// branchbyte2)
	final static short	ifge			= 0x9c;			// branchbyte1,
														// branchbyte2
	// value ? if value is greater
	// than or equal to 0, branch to
	// instruction at branchoffset
	// (signed short constructed
	// from unsigned bytes
	// branchbyte1 << 8 +
	// branchbyte2)
	final static short	ifgt			= 0x9d;			// branchbyte1,
														// branchbyte2
	// value ? if value is greater
	// than 0, branch to instruction
	// at branchoffset (signed short
	// constructed from unsigned
	// bytes branchbyte1 << 8 +
	// branchbyte2)
	final static short	ifle			= 0x9e;			// branchbyte1,
														// branchbyte2
	// value ? if value is less than
	// or equal to 0, branch to
	// instruction at branchoffset
	// (signed short constructed
	// from unsigned bytes
	// branchbyte1 << 8 +
	// branchbyte2)
	final static short	if_icmpeq		= 0x9f;			// branchbyte1,
														// branchbyte2
	// value1, value2 ? if ints are
	// equal, branch to instruction
	// at branchoffset (signed short
	// constructed from unsigned
	// bytes branchbyte1 << 8 +
	// branchbyte2)
	final static short	if_icmpne		= 0xa0;			// branchbyte1,
														// branchbyte2
	// value1, value2 ? if ints are
	// not equal, branch to
	// instruction at branchoffset
	// (signed short constructed
	// from unsigned bytes
	// branchbyte1 << 8 +
	// branchbyte2)
	final static short	if_icmplt		= 0xa1;			// branchbyte1,
														// branchbyte2
	// value1, value2 ? if value1 is
	// less than value2, branch to
	// instruction at branchoffset
	// (signed short constructed
	// from unsigned bytes
	// branchbyte1 << 8 +
	// branchbyte2)
	final static short	if_icmpge		= 0xa2;			// branchbyte1,
														// branchbyte2
	// value1, value2 ? if value1 is
	// greater than or equal to
	// value2, branch to instruction
	// at branchoffset (signed short
	// constructed from unsigned
	// bytes branchbyte1 << 8 +
	// branchbyte2)
	final static short	if_icmpgt		= 0xa3;			// branchbyte1,
														// branchbyte2
	// value1, value2 ? if value1 is
	// greater than value2, branch
	// to instruction at
	// branchoffset (signed short
	// constructed from unsigned
	// bytes branchbyte1 << 8 +
	// branchbyte2)
	final static short	if_icmple		= 0xa4;			// branchbyte1,
														// branchbyte2
	// value1, value2 ? if value1 is
	// less than or equal to value2,
	// branch to instruction at
	// branchoffset (signed short
	// constructed from unsigned
	// bytes branchbyte1 << 8 +
	// branchbyte2)
	final static short	if_acmpeq		= 0xa5;			// branchbyte1,
														// branchbyte2
	// value1, value2 ? if
	// references are equal, branch
	// to instruction at
	// branchoffset (signed short
	// constructed from unsigned
	// bytes branchbyte1 << 8 +
	// branchbyte2)
	final static short	if_acmpne		= 0xa6;			// branchbyte1,
														// branchbyte2
	// value1, value2 ? if
	// references are not equal,
	// branch to instruction at
	// branchoffset (signed short
	// constructed from unsigned
	// bytes branchbyte1 << 8 +
	// branchbyte2)
	final static short	goto_			= 0xa7;			// branchbyte1,
														// branchbyte2 [no
	// change] goes to another
	// instruction at branchoffset
	// (signed short constructed
	// from unsigned bytes
	// branchbyte1 << 8 +
	// branchbyte2)
	final static short	jsr				= 0xa8;			// branchbyte1,
														// branchbyte2 ?
	// address jump to subroutine at
	// branchoffset (signed short
	// constructed from unsigned
	// bytes branchbyte1 << 8 +
	// branchbyte2) and place the
	// return address on the stack
	final static short	ret				= 0xa9;			// index [No change]
														// continue
	// execution from address taken
	// from a local variable #index
	// (the asymmetry with jsr is
	// intentional)
	final static short	tableswitch		= 0xaa;			// [0-3 bytes padding],
	// defaultbyte1, defaultbyte2,
	// defaultbyte3, defaultbyte4,
	// lowbyte1, lowbyte2, lowbyte3,
	// lowbyte4, highbyte1,
	// highbyte2, highbyte3,
	// highbyte4, jump offsets...
	// index ? continue execution
	// from an address in the table
	// at offset index
	final static short	lookupswitch	= 0xab;			// <0-3 bytes padding>,
	// defaultbyte1, defaultbyte2,
	// defaultbyte3, defaultbyte4,
	// npairs1, npairs2, npairs3,
	// npairs4, match-offset
	// pairs... key ? a target
	// address is looked up from a
	// table using a key and
	// execution continues from the
	// instruction at that address
	final static short	ireturn			= 0xac;			// value ? [empty]
														// returns an
	// integer from a method
	final static short	lreturn			= 0xad;			// value ? [empty]
														// returns a
	// long value
	final static short	freturn			= 0xae;			// value ? [empty]
														// returns a
	// float
	final static short	dreturn			= 0xaf;			// value ? [empty]
														// returns a
	// double from a method
	final static short	areturn			= 0xb0;			// objectref ? [empty]
														// returns a
	// reference from a method
	final static short	return_			= 0xb1;			// ? [empty] return void
														// from
	// method
	final static short	getstatic		= 0xb2;			// index1, index2 ?
														// value gets a
	// static field value of a
	// class, where the field is
	// identified by field reference
	// in the constant pool index
	// (index1 << 8 + index2)
	final static short	putstatic		= 0xb3;			// indexbyte1,
														// indexbyte2 value
	// ? set static field to value
	// in a class, where the field
	// is identified by a field
	// reference index in constant
	// pool (indexbyte1 << 8 +
	// indexbyte2)
	final static short	getfield		= 0xb4;			// index1, index2
														// objectref ?
	// value gets a field value of
	// an object objectref, where
	// the field is identified by
	// field reference in the
	// constant pool index (index1
	// << 8 + index2)
	final static short	putfield		= 0xb5;			// indexbyte1,
														// indexbyte2
	// objectref, value ? set field
	// to value in an object
	// objectref, where the field is
	// identified by a field
	// reference index in constant
	// pool (indexbyte1 << 8 +
	// indexbyte2)
	final static short	invokevirtual	= 0xb6;			// indexbyte1,
														// indexbyte2
	// objectref, [arg1, arg2, ...]
	// ? invoke virtual method on
	// object objectref, where the
	// method is identified by
	// method reference index in
	// constant pool (indexbyte1 <<
	// 8 + indexbyte2)
	final static short	invokespecial	= 0xb7;			// indexbyte1,
														// indexbyte2
	// objectref, [arg1, arg2, ...]
	// ? invoke instance method on
	// object objectref, where the
	// method is identified by
	// method reference index in
	// constant pool (indexbyte1 <<
	// 8 + indexbyte2)
	final static short	invokestatic	= 0xb8;			// indexbyte1,
														// indexbyte2 [arg1,
	// arg2, ...] ? invoke a static
	// method, where the method is
	// identified by method
	// reference index in constant
	// pool (indexbyte1 << 8 +
	// indexbyte2)
	final static short	invokeinterface	= 0xb9;			// indexbyte1,
														// indexbyte2,
	// count, 0 objectref, [arg1,
	// arg2, ...] ? invokes an
	// interface method on object
	// objectref, where the
	// interface method is
	// identified by method
	// reference index in constant
	// pool (indexbyte1 << 8 +
	// indexbyte2)
	final static short	xxxunusedxxx	= 0xba;			// this opcode is
														// reserved "for
	// historical reasons"
	final static short	new_			= 0xbb;			// indexbyte1,
														// indexbyte2 ?
	// objectref creates new object
	// of type identified by class
	// reference in constant pool
	// index (indexbyte1 << 8 +
	// indexbyte2)
	final static short	newarray		= 0xbc;			// atype count ?
														// arrayref
	// creates new array with count
	// elements of primitive type
	// identified by atype
	final static short	anewarray		= 0xbd;			// indexbyte1,
														// indexbyte2 count
	// ? arrayref creates a new
	// array of references of length
	// count and component type
	// identified by the class
	// reference index (indexbyte1
	// << 8 + indexbyte2) in the
	// constant pool
	final static short	arraylength		= 0xbe;			// arrayref ? length
														// gets the
	// length of an array
	final static short	athrow			= 0xbf;			// objectref ? [empty],
	// objectref throws an error or
	// exception (notice that the
	// rest of the stack is cleared,
	// leaving only a reference to
	// the Throwable)
	final static short	checkcast		= 0xc0;			// indexbyte1,
														// indexbyte2
	// objectref ? objectref checks
	// whether an objectref is of a
	// certain type, the class
	// reference of which is in the
	// constant pool at index
	// (indexbyte1 << 8 +
	// indexbyte2)
	final static short	instanceof_		= 0xc1;			// indexbyte1,
														// indexbyte2
	// objectref ? result determines
	// if an object objectref is of
	// a given type, identified by
	// class reference index in
	// constant pool (indexbyte1 <<
	// 8 + indexbyte2)
	final static short	monitorenter	= 0xc2;			// objectref ? enter
														// monitor for
	// object ("grab the lock" -
	// start of synchronized()
	// section)
	final static short	monitorexit		= 0xc3;			// objectref ? exit
														// monitor for
	// object ("release the lock" -
	// end of synchronized()
	// section)
	final static short	wide			= 0xc4;			// opcode, indexbyte1,
	// indexbyte2
	final static short	multianewarray	= 0xc5;			// indexbyte1,
														// indexbyte2,
	// dimensions count1,
	// [count2,...] ? arrayref
	// create a new array of
	// dimensions dimensions with
	// elements of type identified
	// by class reference in
	// constant pool index
	// (indexbyte1 << 8 +
	// indexbyte2); the sizes of
	// each dimension is identified
	// by count1, [count2, etc]
	final static short	ifnull			= 0xc6;			// branchbyte1,
														// branchbyte2
	// value ? if value is null,
	// branch to instruction at
	// branchoffset (signed short
	// constructed from unsigned
	// bytes branchbyte1 << 8 +
	// branchbyte2)
	final static short	ifnonnull		= 0xc7;			// branchbyte1,
														// branchbyte2
	// value ? if value is not null,
	// branch to instruction at
	// branchoffset (signed short
	// constructed from unsigned
	// bytes branchbyte1 << 8 +
	// branchbyte2)
	final static short	goto_w			= 0xc8;			// branchbyte1,
														// branchbyte2,
	// branchbyte3, branchbyte4 [no
	// change] goes to another
	// instruction at branchoffset
	// (signed int constructed from
	// unsigned bytes branchbyte1 <<
	// 24 + branchbyte2 << 16 +
	// branchbyte3 << 8 +
	// branchbyte4)
	final static short	jsr_w			= 0xc9;			// branchbyte1,
														// branchbyte2,
	// branchbyte3, branchbyte4 ?
	// address jump to subroutine at
	// branchoffset (signed int
	// constructed from unsigned
	// bytes branchbyte1 << 24 +
	// branchbyte2 << 16 +
	// branchbyte3 << 8 +
	// branchbyte4) and place the
	// return address on the stack
	final static short	breakpoint		= 0xca;			// reserved for
														// breakpoints in
	// Java debuggers; should not
	// appear in any class file
	final static short	impdep1			= 0xfe;			// reserved for
	// implementation-dependent
	// operations within debuggers;
	// should not appear in any
	// class file
	final static short	impdep2			= 0xff;			// reserved for
	// implementation-dependent
	// operations within debuggers;
	// should not appear in any
	// class file

	final static byte	OFFSETS[]		= new byte[256];

	static {
		OFFSETS[bipush] = 1; // byte ? value pushes a byte onto the
		// stack as an integer value
		OFFSETS[sipush] = 2; // byte1, byte2 ? value pushes a signed
		// integer (byte1 << 8 + byte2) onto the
		// stack
		OFFSETS[ldc] = 1; // index ? value pushes a constant
		// #index from a constant pool (String,
		// int, float or class type) onto the
		// stack
		OFFSETS[ldc_w] = 2; // indexbyte1, indexbyte2 ? value pushes
		// a constant #index from a constant
		// pool (String, int, float or class
		// type) onto the stack (wide index is
		// constructed as indexbyte1 << 8 +
		// indexbyte2)
		OFFSETS[ldc2_w] = 2; // indexbyte1, indexbyte2 ? value pushes
		// a constant #index from a constant
		// pool (double or long) onto the stack
		// (wide index is constructed as
		// indexbyte1 << 8 + indexbyte2)
		OFFSETS[iload] = 1; // index ? value loads an int value from
		// a variable #index
		OFFSETS[lload] = 1; // index ? value load a long value from
		// a local variable #index
		OFFSETS[fload] = 1; // index ? value loads a float value
		// from a local variable #index
		OFFSETS[dload] = 1; // index ? value loads a double value
		// from a local variable #index
		OFFSETS[aload] = 1; // index ? objectref loads a reference
		// onto the stack from a local variable
		// #index
		OFFSETS[istore] = 1; // index value ? store int value into
		// variable #index
		OFFSETS[lstore] = 1; // index value ? store a long value in a
		// local variable #index
		OFFSETS[fstore] = 1; // index value ? stores a float value
		// into a local variable #index
		OFFSETS[dstore] = 1; // index value ? stores a double value
		// into a local variable #index
		OFFSETS[iinc] = 2; // index, const [No change] increment
		// local variable #index by signed byte
		// const
		OFFSETS[ifeq] = 2; // branchbyte1, branchbyte2 value ? if
		// value is 0, branch to instruction at
		// branchoffset (signed short
		// constructed from unsigned bytes
		// branchbyte1 << 8 + branchbyte2)
		OFFSETS[astore] = 1; // index objectref ? stores a reference
		// into a local variable #index
		OFFSETS[ifne] = 2; // branchbyte1, branchbyte2 value ? if
		// value is not 0, branch to instruction
		// at branchoffset (signed short
		// constructed from unsigned bytes
		// branchbyte1 << 8 + branchbyte2)
		OFFSETS[iflt] = 2; // branchbyte1, branchbyte2 value ? if
		// value is less than 0, branch to
		// instruction at branchoffset (signed
		// short constructed from unsigned bytes
		// branchbyte1 << 8 + branchbyte2)
		OFFSETS[ifge] = 2; // branchbyte1, branchbyte2 value ? if
		// value is greater than or equal to 0,
		// branch to instruction at branchoffset
		// (signed short constructed from
		// unsigned bytes branchbyte1 << 8 +
		// branchbyte2)
		OFFSETS[ifgt] = 2; // branchbyte1, branchbyte2 value ? if
		// value is greater than 0, branch to
		// instruction at branchoffset (signed
		// short constructed from unsigned bytes
		// branchbyte1 << 8 + branchbyte2)
		OFFSETS[ifle] = 2; // branchbyte1, branchbyte2 value ? if
		// value is less than or equal to 0,
		// branch to instruction at branchoffset
		// (signed short constructed from
		// unsigned bytes branchbyte1 << 8 +
		// branchbyte2)
		OFFSETS[if_icmpeq] = 2; // branchbyte1, branchbyte2 value1,
		// value2 ? if ints are equal,
		// branch to instruction at
		// branchoffset (signed short
		// constructed from unsigned bytes
		// branchbyte1 << 8 + branchbyte2)
		OFFSETS[if_icmpne] = 2; // branchbyte1, branchbyte2 value1,
		// value2 ? if ints are not equal,
		// branch to instruction at
		// branchoffset (signed short
		// constructed from unsigned bytes
		// branchbyte1 << 8 + branchbyte2)
		OFFSETS[if_icmplt] = 2; // branchbyte1, branchbyte2 value1,
		// value2 ? if value1 is less than
		// value2, branch to instruction at
		// branchoffset (signed short
		// constructed from unsigned bytes
		// branchbyte1 << 8 + branchbyte2)
		OFFSETS[if_icmpge] = 2; // branchbyte1, branchbyte2 value1,
		// value2 ? if value1 is greater
		// than or equal to value2, branch
		// to instruction at branchoffset
		// (signed short constructed from
		// unsigned bytes branchbyte1 << 8 +
		// branchbyte2)
		OFFSETS[if_icmpgt] = 2; // branchbyte1, branchbyte2 value1,
		// value2 ? if value1 is greater
		// than value2, branch to
		// instruction at branchoffset
		// (signed short constructed from
		// unsigned bytes branchbyte1 << 8 +
		// branchbyte2)
		OFFSETS[if_icmple] = 2; // branchbyte1, branchbyte2 value1,
		// value2 ? if value1 is less than
		// or equal to value2, branch to
		// instruction at branchoffset
		// (signed short constructed from
		// unsigned bytes branchbyte1 << 8 +
		// branchbyte2)
		OFFSETS[if_acmpeq] = 2; // branchbyte1, branchbyte2 value1,
		// value2 ? if references are equal,
		// branch to instruction at
		// branchoffset (signed short
		// constructed from unsigned bytes
		// branchbyte1 << 8 + branchbyte2)
		OFFSETS[if_acmpne] = 2; // branchbyte1, branchbyte2 value1,
		// value2 ? if references are not
		// equal, branch to instruction at
		// branchoffset (signed short
		// constructed from unsigned bytes
		// branchbyte1 << 8 + branchbyte2)
		OFFSETS[goto_] = 2; // branchbyte1, branchbyte2 [no change]
		// goes to another instruction at
		// branchoffset (signed short
		// constructed from unsigned bytes
		// branchbyte1 << 8 + branchbyte2)
		OFFSETS[jsr] = 2; // branchbyte1, branchbyte2 ? address
		// jump to subroutine at branchoffset
		// (signed short constructed from
		// unsigned bytes branchbyte1 << 8 +
		// branchbyte2) and place the return
		// address on the stack
		OFFSETS[ret] = 1; // index [No change] continue execution
		// from address taken from a local
		// variable #index (the asymmetry with
		// jsr is intentional)
		OFFSETS[tableswitch] = -1; // [0-3 bytes padding],
		// defaultbyte1, defaultbyte2,
		// defaultbyte3, defaultbyte4,
		// lowbyte1, lowbyte2, lowbyte3,
		// lowbyte4, highbyte1,
		// highbyte2, highbyte3,
		// highbyte4, jump offsets...
		// index ? continue execution
		// from an address in the table
		// at offset index
		OFFSETS[lookupswitch] = -1; // <0-3 bytes padding>,
		// defaultbyte1, defaultbyte2,
		// defaultbyte3, defaultbyte4,
		// npairs1, npairs2, npairs3,
		// npairs4, match-offset
		// pairs... key ? a target
		// address is looked up from a
		// table using a key and
		// execution continues from the
		// instruction at that address
		OFFSETS[getstatic] = 2; // index1, index2 ? value gets a
		// static field value of a class,
		// where the field is identified by
		// field reference in the constant
		// pool index (index1 << 8 + index2)
		OFFSETS[putstatic] = 2; // indexbyte1, indexbyte2 value ?
		// set static field to value in a
		// class, where the field is
		// identified by a field reference
		// index in constant pool
		// (indexbyte1 << 8 + indexbyte2)
		OFFSETS[getfield] = 2; // index1, index2 objectref ? value
		// gets a field value of an object
		// objectref, where the field is
		// identified by field reference in
		// the constant pool index (index1
		// << 8 + index2)
		OFFSETS[putfield] = 2; // indexbyte1, indexbyte2 objectref,
		// value ? set field to value in an
		// object objectref, where the field
		// is identified by a field
		// reference index in constant pool
		// (indexbyte1 << 8 + indexbyte2)
		OFFSETS[invokevirtual] = 2; // indexbyte1, indexbyte2
		// objectref, [arg1, arg2, ...]
		// ? invoke virtual method on
		// object objectref, where the
		// method is identified by
		// method reference index in
		// constant pool (indexbyte1 <<
		// 8 + indexbyte2)
		OFFSETS[invokespecial] = 2; // indexbyte1, indexbyte2
		// objectref, [arg1, arg2, ...]
		// ? invoke instance method on
		// object objectref, where the
		// method is identified by
		// method reference index in
		// constant pool (indexbyte1 <<
		// 8 + indexbyte2)
		OFFSETS[invokestatic] = 2; // indexbyte1, indexbyte2 [arg1,
		// arg2, ...] ? invoke a static
		// method, where the method is
		// identified by method
		// reference index in constant
		// pool (indexbyte1 << 8 +
		// indexbyte2)
		OFFSETS[invokeinterface] = 2; // indexbyte1, indexbyte2,
		// count, 0 objectref,
		// [arg1, arg2, ...] ?
		// invokes an interface
		// method on object
		// objectref, where the
		// interface method is
		// identified by method
		// reference index in
		// constant pool (indexbyte1
		// << 8 + indexbyte2)
		OFFSETS[new_] = 2; // indexbyte1, indexbyte2 ? objectref
		// creates new object of type identified
		// by class reference in constant pool
		// index (indexbyte1 << 8 + indexbyte2)
		OFFSETS[newarray] = 1; // atype count ? arrayref creates
		// new array with count elements of
		// primitive type identified by
		// atype
		OFFSETS[anewarray] = 2; // indexbyte1, indexbyte2 count ?
		// arrayref creates a new array of
		// references of length count and
		// component type identified by the
		// class reference index (indexbyte1
		// << 8 + indexbyte2) in the
		// constant pool
		OFFSETS[checkcast] = 2; // indexbyte1, indexbyte2 objectref
		// ? objectref checks whether an
		// objectref is of a certain type,
		// the class reference of which is
		// in the constant pool at index
		// (indexbyte1 << 8 + indexbyte2)
		OFFSETS[instanceof_] = 2; // indexbyte1, indexbyte2 objectref
		// ? result determines if an object
		// objectref is of a given type,
		// identified by class reference
		// index in constant pool
		// (indexbyte1 << 8 + indexbyte2)
		OFFSETS[wide] = 3; // opcode, indexbyte1, indexbyte2
		OFFSETS[multianewarray] = 3; // indexbyte1, indexbyte2,
		// dimensions count1,
		// [count2,...] ? arrayref
		// create a new array of
		// dimensions dimensions with
		// elements of type identified
		// by class reference in
		// constant pool index
		// (indexbyte1 << 8 +
		// indexbyte2); the sizes of
		// each dimension is identified
		// by count1, [count2, etc]
		OFFSETS[ifnull] = 2; // branchbyte1, branchbyte2 value ? if
		// value is null, branch to instruction
		// at branchoffset (signed short
		// constructed from unsigned bytes
		// branchbyte1 << 8 + branchbyte2)
		OFFSETS[ifnonnull] = 2; // branchbyte1, branchbyte2 value ?
		// if value is not null, branch to
		// instruction at branchoffset
		// (signed short constructed from
		// unsigned bytes branchbyte1 << 8 +
		// branchbyte2)
		OFFSETS[goto_w] = 4; // branchbyte1, branchbyte2,
		// branchbyte3, branchbyte4 [no change]
		// goes to another instruction at
		// branchoffset (signed int constructed
		// from unsigned bytes branchbyte1 << 24
		// + branchbyte2 << 16 + branchbyte3 <<
		// 8 + branchbyte4)
		OFFSETS[jsr_w] = 4; // branchbyte1, branchbyte2,
		// branchbyte3, branchbyte4 ? address
		// jump to subroutine at branchoffset
		// (signed int constructed from unsigned
		// bytes branchbyte1 << 24 + branchbyte2
		// << 16 + branchbyte3 << 8 +
		// branchbyte4) and place the return
		// address on the stack
	}

}
