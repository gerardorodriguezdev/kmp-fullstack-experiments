#ifndef CommunicationType_h
#define CommunicationType_h

#include <Arduino.h>

constexpr unsigned int COMMUNICATION_TYPE_CODE_SIZE = 2;

enum class CommunicationType {
    INVALID,
    META_DATA,
    DATA,
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
#endif //CommunicationType_h
