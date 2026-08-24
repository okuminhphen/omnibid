"use client";

import { useCallback, useEffect } from "react";
import { useRouter } from "next/navigation";
import { GoogleOneTap } from "@/components/GoogleOneTap";
import { useAuth } from "@/components/AuthProvider";

export default function LoginPage() {
  const router = useRouter();
  const { user, hydrated } = useAuth();
  const finishLogin = useCallback(() => router.replace("/"), [router]);

  useEffect(() => {
    if (hydrated && user) finishLogin();
  }, [finishLogin, hydrated, user]);

  return (
    <main className="min-h-[calc(100vh-4.5rem)] border-b border-stone-300">
      <div className="mx-auto grid max-w-[1200px] lg:grid-cols-[0.9fr_1.1fr]">
        <section className="flex min-h-[390px] flex-col justify-between border-b border-stone-300 px-5 py-12 sm:px-8 sm:py-16 lg:min-h-[680px] lg:border-b-0 lg:border-r lg:px-12 lg:py-20">
          <div>
            <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-[#b43a2f]">Tài khoản OmniBid</p>
            <h1 className="display-serif mt-5 max-w-md text-5xl leading-[1.02] tracking-[-0.04em] text-stone-950 sm:text-6xl">
              Một tài khoản cho toàn bộ hành trình đấu giá.
            </h1>
            <p className="mt-6 max-w-sm text-base leading-7 text-stone-600">
              Theo dõi phiên đấu giá, quản lý tiền cọc và xem lại lịch sử giao dịch của riêng bạn.
            </p>
          </div>
          <div className="mt-12 grid max-w-md grid-cols-2 gap-6 border-t border-stone-300 pt-5 text-xs leading-5 text-stone-500">
            <p><span className="block font-semibold text-stone-800">Không cần mật khẩu</span>Google xác thực danh tính của bạn.</p>
            <p><span className="block font-semibold text-stone-800">Quyền riêng biệt</span>Mỗi người có ví và lịch sử riêng.</p>
          </div>
        </section>

        <section className="flex items-center px-5 py-12 sm:px-8 lg:px-16">
          <div className="w-full max-w-md lg:mx-auto">
            <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-stone-500">Đăng nhập hoặc đăng ký</p>
            <h2 className="display-serif mt-3 text-4xl tracking-[-0.025em] text-stone-950">Tiếp tục với Google</h2>
            <p className="mt-3 text-sm leading-6 text-stone-500">
              Nếu email chưa tồn tại, OmniBid sẽ tự tạo một tài khoản khách hàng mới sau khi Google xác thực thành công.
            </p>

            <div className="mt-8 border-y border-stone-300 py-8">
              <GoogleOneTap onSuccess={finishLogin} />
            </div>

            <div className="mt-7 space-y-4 text-xs leading-5 text-stone-500">
              <p>
                Bằng việc tiếp tục, bạn đồng ý để OmniBid lưu hồ sơ cơ bản và các phiên đăng nhập phục vụ bảo mật tài khoản.
              </p>
              <p className="border-l-2 border-[#b43a2f] pl-4">
                Tài khoản quản trị không được tạo từ giao diện. Quyền ADMIN chỉ được cấp từ cấu hình bảo mật của identity-service.
              </p>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
