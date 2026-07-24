import { describe, expect, it } from "vitest";
import { compactText, formatCurrency, getErrorMessage } from "./format";

describe("format helpers", () => {
  it("formats VND without decimal places", () => {
    expect(formatCurrency(125000)).toContain("125.000");
  });

  it("shortens long text", () => {
    expect(compactText("Sản phẩm chăm sóc thú cưng", 12)).toBe("Sản phẩm chă…");
  });

  it("prefers the backend error message", () => {
    const error = {
      message: "Network error",
      response: { data: { message: "Mã giảm giá không hợp lệ" } },
    };
    expect(getErrorMessage(error)).toBe("Mã giảm giá không hợp lệ");
  });
});
