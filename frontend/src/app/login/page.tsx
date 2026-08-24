"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { GoogleOneTap } from "@/components/GoogleOneTap";
import { useAuth } from "@/components/AuthProvider";

const DEV_ACCOUNTS = [
  { alias: "customer-a", label: "Customer A", note: "Dùng cửa sổ thường" },
  { alias: "customer-b", label: "Customer B", note: "Dùng cửa sổ ẩn danh" },
  { alias: "admin", label: "Administrator", note: "Role ADMIN" }
];

export default function LoginPage() {
  const router = useRouter();
  const { user, hydrated, loginDev } = useAuth();
  const [submitting, setSubmitting] = useState<string | null>(null);
  const [error, setError] = useState("");
  const finishLogin = useCallback(() => router.replace("/"), [router]);

  useEffect(() => {
    if (hydrated && user) finishLogin();
  }, [finishLogin, hydrated, user]);

  async function login(alias: string) {
    setSubmitting(alias);
    setError("");
    try {
      await loginDev(alias);
      finishLogin();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Đăng nhập thất bại.");
    } finally {
      setSubmitting(null);
    }
  }

  return (
    <main className="min-h-[calc(100vh-4.5rem)] border-b border-stone-300">
      <div className="mx-auto grid max-w-[1200px] lg:grid-cols-2">
        <section className="flex min-h-[420px] flex-col justify-between border-b border-stone-300 px-5 py-12 sm:px-8 sm:py-16 lg:min-h-[680px] lg:border-b-0 lg:border-r lg:px-12 lg:py-20">
          <div>
            <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-[#b43a2f]">Tài khoản người đấu giá</p>
            <h1 className="display-serif mt-5 max-w-md text-5xl leading-[1.02] tracking-[-0.04em] text-stone-950 sm:text-6xl">Chào mừng bạn trở lại.</h1>
            <p className="mt-6 max-w-sm text-base leading-7 text-stone-600">Đăng nhập để tham gia đặt giá, quản lý tiền cọc và theo dõi các giao dịch của bạn.</p>
          </div>
          <p className="mt-12 max-w-sm border-t border-stone-300 pt-5 text-xs leading-6 text-stone-500">Thông tin phiên đăng nhập được bảo vệ và có thể thu hồi bất cứ lúc nào trong trang hồ sơ.</p>
        </section>

        <section className="flex items-center px-5 py-12 sm:px-8 lg:px-12">
          <div className="w-full max-w-md lg:mx-auto">
            <h2 className="display-serif text-3xl text-stone-950">Đăng nhập</h2>
            <p className="mt-2 text-sm text-stone-500">Tiếp tục bằng Google hoặc chọn tài khoản thử nghiệm.</p>
            <div className="mt-8">
              <GoogleOneTap onSuccess={finishLogin} />
            </div>

            <div className="my-8 flex items-center gap-4 text-[9px] font-semibold uppercase tracking-[0.16em] text-stone-400">
              <span className="h-px flex-1 bg-stone-300" />
              Tài khoản dùng thử
              <span className="h-px flex-1 bg-stone-300" />
            </div>

            <div className="border-t border-stone-300">
              {DEV_ACCOUNTS.map((account) => (
                <button
                  key={account.alias}
                  type="button"
                  disabled={Boolean(submitting)}
                  onClick={() => void login(account.alias)}
                  className="flex w-full items-center justify-between border-b border-stone-300 py-4 text-left transition hover:pl-2 disabled:opacity-50"
                >
                  <span>
                    <span className="block text-sm font-semibold text-stone-900">{account.label}</span>
                    <span className="mt-1 block text-xs text-stone-500">{account.note}</span>
                  </span>
                  <span className="text-stone-500">{submitting === account.alias ? "Đang vào..." : "→"}</span>
                </button>
              ))}
            </div>

            {error && <p className="mt-5 border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-800">{error}</p>}
            <p className="mt-6 text-xs leading-5 text-stone-500">Tài khoản dùng thử chỉ hoạt động trong môi trường local.</p>
          </div>
        </section>
      </div>
    </main>
  );
}
