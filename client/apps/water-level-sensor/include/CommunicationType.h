#ifndef CommunicationType_h
#define CommunicationType_h

#include <Arduino.h>

constexpr unsigned int COMMUNICATION_TYPE_CODE_SIZE = 2;

enum class CommunicationType {
    META_DATA = 0,
    DATA = 1,
    INVALID = 2,
};

namespace detail {
    struct CommunicationTypeEntry {
        CommunicationType type;
        const char *code;
    };

    constexpr CommunicationTypeEntry communicationTypeEntries[] = {
        {CommunicationType::META_DATA, "MD"},
        {CommunicationType::DATA, "DT"},
    };
}

constexpr CommunicationType stringToCommunicationType(const char *string) {
    if (string == nullptr) return CommunicationType::INVALID;

    for (const auto &[type, code]: detail::communicationTypeEntries) {
        if (strcmp(string, code) == 0) return type;
    }

    return CommunicationType::INVALID;
}

constexpr const char *communicationTypeToString(const CommunicationType type) {
    return detail::communicationTypeEntries[static_cast<int>(type)].code;
}

#endif //CommunicationType_h
