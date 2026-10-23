import {isPositiveDecimalIdentifier} from "@/lib/business-identifier";
import {describe, expect, it} from "vitest";

describe("isPositiveDecimalIdentifier", () => {
    it.each([
        "1",
        "100301",
        "9007199254740993",
        "9223372036854775807",
    ])("应接受十进制正整数标识 %s", (value) => {
        // Given / When
        const result = isPositiveDecimalIdentifier(value);

        // Then
        expect(result).toBe(true);
    });

    it.each([
        "",
        "0",
        "001",
        "-1",
        "+1",
        "1.5",
        "1e10",
        " 1",
        "1 ",
        "activity-1",
    ])("应拒绝非规范字符串标识 %j", (value) => {
        // Given / When
        const result = isPositiveDecimalIdentifier(value);

        // Then
        expect(result).toBe(false);
    });

    it.each([null, undefined, 1, Symbol("business-id"), {}, []])("应拒绝非字符串值 %s", (value) => {
        // Given / When
        const result = isPositiveDecimalIdentifier(value);

        // Then
        expect(result).toBe(false);
    });
});
