#include "stdafx.h"
#include "jfield.h"

#include <cstdint>
#include <cstring>
#if defined(_MSC_VER)
#include <atomic>
#endif

Field::Field()
{
	fieldName = NULL;
	fieldDesc = NULL;
	constValue = NULL;
	attrCount = 0;
	accFlag = 0;
	simple_name = NULL;
	slot = -1;
	static_slot = -1;
}

Field::~Field()
{
	if (constValue)
	{
		delete constValue;
		constValue = NULL;
	}
	if (simple_name)
	{
		delete[] simple_name;
		simple_name = NULL;
	}
}

LPSTR &Field::GetSimpleName()
{
	if (!simple_name)
	{
		LPCSTR name = fieldName ? fieldName->GetName() : "";
		LPCSTR desc = fieldDesc ? fieldDesc->GetName() : "";
		if (!name) name = "";
		if (!desc) desc = "";
		size_t len = strlen(name) + strlen(desc) + 2;
		simple_name = new CHAR[len];
		snprintf(simple_name, len, "%s:%s", name, desc);
	}
	return simple_name;
}

FieldValue::FieldValue()
{
	field = NULL;
	field_value.dValue = 0;
	field_value.iValue = 0;
}

FieldValue::FieldValue(lpField field)
{
	field_value.dValue = 0;
	field_value.iValue = 0;
	this->field = field;
}

void FieldValue::initialize()
{
	if (!field)
	{
		return;
	}
	if (!field->constValue)
	{
		return;
	}
	lpConstant constant = field->constValue->constant;
	if (!constant)
	{
		return;
	}
	U1 tag = constant->tag;
	switch (tag)
	{
	case INTEGER_CONSTANT:
		{
			IntegerConstant *cv = (IntegerConstant *)constant;
			field_value.iValue = static_cast<int>(cv->bytes);
			break;
		}
	case FLOAT_CONSTANT:
		{
			FloatConstant *cv = (FloatConstant *)constant;
			field_value.set(cv->bytes, 4);
			break;
		}
	case LONG_CONSTANT:
		{
			LongConstant *cv = (LongConstant *)constant;
			WByteValue bv(cv->bytes1, cv->bytes2);
			field_value.lValue = bv.lValue;
			break;
		}
	case DOUBLE_CONSTANT:
		{
			DoubleConstant *cv = (DoubleConstant *)constant;
			WByteValue bv(cv->bytes2, cv->bytes1);
			field_value.dValue = bv.dValue;
			break;
		}
	case STRING_CONSTANT:
		{
			StringConstant *st = (StringConstant*)constant;
			field_value.heapPtr = st->GetString();
			break;
		}
	default:
		break;
	}
}

namespace {

inline uint64_t AtomicLoad64(const void *src, bool seq_cst)
{
#if defined(_MSC_VER)
	const std::atomic<uint64_t> *p =
		reinterpret_cast<const std::atomic<uint64_t> *>(src);
	return p->load(seq_cst ? std::memory_order_seq_cst
	                      : std::memory_order_relaxed);
#else
	return __atomic_load_n(reinterpret_cast<const uint64_t *>(src),
	                       seq_cst ? __ATOMIC_SEQ_CST : __ATOMIC_RELAXED);
#endif
}

inline void AtomicStore64(void *dst, uint64_t value, bool seq_cst)
{
#if defined(_MSC_VER)
	std::atomic<uint64_t> *p = reinterpret_cast<std::atomic<uint64_t> *>(dst);
	p->store(value, seq_cst ? std::memory_order_seq_cst
	                        : std::memory_order_relaxed);
#else
	__atomic_store_n(reinterpret_cast<uint64_t *>(dst), value,
	                 seq_cst ? __ATOMIC_SEQ_CST : __ATOMIC_RELAXED);
#endif
}

} // namespace

Variant FieldValue::LoadValue() const
{
	static_assert(sizeof(Variant) == sizeof(uint64_t),
	              "field values must fit one atomic 64-bit word");
	bool seq_cst = field != NULL && (field->accFlag & ACF_VOLATILE) != 0;
	uint64_t bits = AtomicLoad64(&field_value, seq_cst);
	Variant out;
	memcpy(&out, &bits, sizeof(out));
	return out;
}

void FieldValue::StoreValue(const Variant &value)
{
	bool seq_cst = field != NULL && (field->accFlag & ACF_VOLATILE) != 0;
	uint64_t bits = 0;
	memcpy(&bits, &value, sizeof(bits));
	AtomicStore64(&field_value, bits, seq_cst);
}
