# Tinh Tinh - Ứng Dụng Theo Dõi Biến Động Số Dư

## Giới thiệu

**Tinh Tinh** là ứng dụng Android giúp theo dõi thông báo biến động số dư từ các ứng dụng ngân hàng. Ứng dụng sẽ tự động đọc các thông báo và thông báo cho bạn khi có tiền được chuyển vào tài khoản.

## Tính năng chính

-   **Theo dõi biến động số dư**: Tự động phát hiện thông báo biến động từ các ứng dụng ngân hàng
-   **Thông báo nhận tiền**: Hiển thị và thông báo khi bạn nhận được tiền
-   **Đọc thông báo**: Sử dụng Text-to-Speech để đọc thông báo "Bạn đã nhận được [số tiền]"
-   **Lịch sử giao dịch**: Lưu lại lịch sử các giao dịch nhận tiền
-   **Xóa thông báo**: Xóa từng thông báo hoặc tất cả thông báo
-   **Giao diện hiện đại**: Thiết kế Material Design với giao diện card layout trực quan
-   **Tùy chỉnh thông báo**: Cho phép thay đổi tiền tố thông báo theo ý thích
-   **Hỗ trợ MB Bank**: Nhận dạng thông báo từ MB Bank với các mẫu thông báo cụ thể

## Cách sử dụng

1. **Cài đặt ứng dụng**: Tải và cài đặt ứng dụng Tinh Tinh từ file APK
2. **Cấp quyền truy cập thông báo**:

    - Khi khởi động lần đầu, ứng dụng sẽ yêu cầu quyền đọc thông báo
    - Bạn có thể cấp quyền bằng cách nhấn nút cấp quyền trong ứng dụng
    - Trong màn hình cài đặt, tìm và bật quyền cho Tinh Tinh trong danh sách

3. **Cài đặt Vietnamese TTS (nếu cần)**:

    - Ứng dụng sẽ tự động thông báo khi cần cài đặt gói ngôn ngữ tiếng Việt cho TTS

4. **Tùy chỉnh trong màn hình cài đặt**:

    - Nhấn nút cài đặt để truy cập vào màn hình cài đặt
    - Thay đổi tiền tố thông báo theo ý thích (ví dụ: "Tài khoản vừa nhận", "Tiền về rồi nè", v.v.)
    - Điều chỉnh các tùy chọn khác theo nhu cầu

5. **Sử dụng**:

    - Khi có thông báo biến động số dư, ứng dụng sẽ hiển thị và đọc thông báo
    - Lịch sử giao dịch sẽ được hiển thị trong danh sách
    - Xóa thông báo bằng cách vuốt trên từng mục hoặc nhấn nút xóa tất cả

## Hỗ trợ ngân hàng

Ứng dụng hỗ trợ các ngân hàng phổ biến tại Việt Nam:

-   MB Bank (cải tiến nhận dạng)
-   VietinBank

## Quyền yêu cầu

-   **Đọc thông báo**: Ứng dụng cần quyền đọc thông báo để phát hiện biến động số dư
-   **Internet**: Để cập nhật và nhận thông báo khi có mạng

## Lưu ý

-   Ứng dụng chỉ lấy thông tin từ thông báo ngân hàng, không truy cập vào tài khoản ngân hàng của bạn
-   Không yêu cầu thông tin đăng nhập, mật khẩu hay thông tin cá nhân
-   Dữ liệu được lưu trữ cục bộ trên thiết bị, không được chia sẻ lên máy chủ

## Phiên bản

**Phiên bản hiện tại**: 1.1.0

-   Giao diện được thiết kế lại hoàn toàn với Material Design hiện đại
-   Trang chính với giao diện card layout hiển thị tổng số dư
-   Thông báo giao dịch thiết kế giống thông báo ngân hàng
-   Nút được di chuyển từ màn hình chính sang màn hình cài đặt
-   Thêm tính năng xóa từng thông báo hoặc tất cả thông báo
-   Cải tiến nhận dạng thông báo MB Bank bằng regex
-   Thêm tính năng nhắc nhở cài đặt Vietnamese TTS khi chưa có
-   Cho phép cài đặt phát TTS chạy ngầm mở không cần mở app liên tục.
-   Sửa lỗi xung đột theme giữa day/night mode
-   Sửa lỗi ClassCastException khi xử lý FloatingActionButton

## Phát triển

Ứng dụng được phát triển bởi hongducdev
