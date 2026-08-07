import { useState } from "react";
import { Linking } from "react-native";
import { Button } from "../ui/Button";
import { ErrorBanner } from "../ui/Feedback";
import { ApiError } from "../../api/client";
import type { DocumentResponse } from "../../types";

export function PdfButton({ onGenerate }: { onGenerate: () => Promise<DocumentResponse> }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handlePress = async () => {
    setError(null);
    setLoading(true);
    try {
      const doc = await onGenerate();
      await Linking.openURL(doc.downloadUrl);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "PDF 발급에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Button label="PDF 다운로드" variant="secondary" onPress={handlePress} loading={loading} />
      <ErrorBanner message={error} />
    </>
  );
}
