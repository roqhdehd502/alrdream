import { diffContent, prettyPath } from "./contentDiff";

describe("diffContent", () => {
  it("동일한 값이면 diff가 없다", () => {
    const result = diffContent({ title: "A" }, { title: "A" });

    expect(result).toEqual([]);
  });

  it("값이 바뀐 필드만 diff로 반환한다", () => {
    const result = diffContent({ title: "A", body: "same" }, { title: "B", body: "same" });

    expect(result).toEqual([{ path: "title", before: "A", after: "B" }]);
  });

  it("이전에 없던 필드는 before가 null이다", () => {
    const result = diffContent({}, { title: "새로 생김" });

    expect(result).toEqual([{ path: "title", before: null, after: "새로 생김" }]);
  });

  it("다음 버전에서 사라진 필드는 after가 null이다", () => {
    const result = diffContent({ title: "사라짐" }, {});

    expect(result).toEqual([{ path: "title", before: "사라짐", after: null }]);
  });

  it("중첩 객체는 경로를 점(.)으로 이어붙인다", () => {
    const result = diffContent({ idea: { pitch: "A" } }, { idea: { pitch: "B" } });

    expect(result).toEqual([{ path: "idea.pitch", before: "A", after: "B" }]);
  });

  it("배열은 1부터 시작하는 인덱스로 경로를 만든다", () => {
    const result = diffContent({ items: ["A"] }, { items: ["A", "B"] });

    expect(result).toEqual([{ path: "items[2]", before: null, after: "B" }]);
  });

  it("빈 배열은 (없음) 문자열로 표시되어 채워진 배열과 다르게 취급된다", () => {
    const result = diffContent({ items: [] }, { items: ["A"] });

    expect(result).toContainEqual({ path: "items[1]", before: null, after: "A" });
    expect(result).toContainEqual({ path: "items", before: "(없음)", after: null });
  });

  it("null/undefined 값은 빈 문자열로 취급해 같으면 diff에 잡히지 않는다", () => {
    const result = diffContent({ title: null }, { title: undefined });

    expect(result).toEqual([]);
  });

  it("결과는 경로 이름순으로 정렬된다", () => {
    const result = diffContent({ b: "1", a: "1" }, { b: "2", a: "2" });

    expect(result.map((r) => r.path)).toEqual(["a", "b"]);
  });
});

describe("prettyPath", () => {
  it("스네이크_케이스를 공백으로 바꾼다", () => {
    expect(prettyPath("one_line_pitch")).toBe("one line pitch");
  });

  it("점(.) 구분 경로를 ' > '로 연결한다", () => {
    expect(prettyPath("idea_summary.one_line_pitch")).toBe("idea summary > one line pitch");
  });

  it("배열 인덱스 표기 [n]을 공백+숫자로 바꾼다", () => {
    expect(prettyPath("items[2].name")).toBe("items 2 > name");
  });
});
