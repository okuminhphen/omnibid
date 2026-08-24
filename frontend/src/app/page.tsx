import Link from "next/link";
import { AuctionList } from "@/components/AuctionList";

export default function HomePage() {
  return (
    <main>
      <section className="border-b border-stone-300">
        <div className="mx-auto grid max-w-[1440px] lg:grid-cols-[1.35fr_0.65fr]">
          <div className="px-5 py-20 sm:px-8 sm:py-28 lg:border-r lg:border-stone-300 lg:px-12 lg:py-36">
            <p className="mb-8 text-[11px] font-semibold uppercase tracking-[0.22em] text-[#b43a2f]">Bộ sưu tập tháng 8 · 2026</p>
            <h1 className="display-serif max-w-4xl text-5xl leading-[0.98] tracking-[-0.045em] text-stone-950 sm:text-7xl lg:text-[92px]">
              Những món đồ đáng để chờ đợi.
            </h1>
            <p className="mt-8 max-w-xl text-base leading-7 text-stone-600 sm:text-lg">
              Các phiên đấu giá tuyển chọn dành cho người yêu thiết kế, công nghệ và những vật phẩm có câu chuyện riêng.
            </p>
            <div className="mt-10 flex items-center gap-6">
              <a href="#live-auctions" className="border-b border-stone-950 pb-1 text-sm font-semibold text-stone-950 transition hover:border-[#b43a2f] hover:text-[#b43a2f]">
                Xem các phiên đang mở
              </a>
              <Link href="/wallet" className="text-sm text-stone-500 transition hover:text-stone-950">
                Kiểm tra ví
              </Link>
            </div>
          </div>
          <div className="flex flex-col justify-between bg-[#262521] px-5 py-12 text-stone-100 sm:px-8 lg:px-10 lg:py-14">
            <div className="flex items-center justify-between border-b border-white/20 pb-4 text-[10px] uppercase tracking-[0.18em] text-stone-400">
              <span>Live now</span>
              <span className="flex items-center gap-2"><span className="size-1.5 rounded-full bg-[#d85d50]" /> Cập nhật trực tiếp</span>
            </div>
            <blockquote className="display-serif my-16 text-3xl leading-tight text-stone-100 lg:text-4xl">
              “Giá trị không chỉ nằm ở món đồ, mà còn ở câu chuyện của người sở hữu tiếp theo.”
            </blockquote>
            <div className="grid grid-cols-2 border-t border-white/20 pt-5 text-sm">
              <div><p className="text-stone-500">Đơn vị</p><p className="mt-1">Việt Nam Đồng</p></div>
              <div><p className="text-stone-500">Hình thức</p><p className="mt-1">Đấu giá trực tuyến</p></div>
            </div>
          </div>
        </div>
      </section>

      <section id="live-auctions" className="mx-auto max-w-[1440px] px-5 py-16 sm:px-8 sm:py-24 lg:px-12">
        <div className="mb-10 flex flex-col justify-between gap-4 border-b border-stone-300 pb-6 sm:flex-row sm:items-end">
          <div>
            <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-stone-500">Danh mục hiện tại</p>
            <h2 className="display-serif mt-2 text-4xl tracking-[-0.03em] text-stone-950 sm:text-5xl">Phiên đang mở</h2>
          </div>
          <p className="max-w-sm text-sm leading-6 text-stone-500">Giá và trạng thái được cập nhật tự động. Chọn một lô để xem hồ sơ và tham gia đặt giá.</p>
        </div>
        <AuctionList />
      </section>
    </main>
  );
}
