interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  onChange: (page: number) => void;
}

export function Pagination({ page, totalPages, totalElements, onChange }: PaginationProps) {
  if (totalElements === 0) return null;

  return (
    <div className="pagination">
      <button type="button" className="btn" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        이전
      </button>
      <span>
        {page + 1} / {Math.max(totalPages, 1)} 페이지 (총 {totalElements}건)
      </span>
      <button type="button" className="btn" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        다음
      </button>
    </div>
  );
}
