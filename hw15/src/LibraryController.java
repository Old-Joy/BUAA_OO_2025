import com.oocourse.library3.LibraryBookId;
import com.oocourse.library3.LibraryBookIsbn;
import com.oocourse.library3.LibraryBookState;
import com.oocourse.library3.LibraryMoveInfo;
import com.oocourse.library3.LibraryQcsCmd;
import com.oocourse.library3.LibraryOpenCmd;
import com.oocourse.library3.LibraryCloseCmd;
import com.oocourse.library3.LibraryReqCmd;
import com.oocourse.library3.annotation.SendMessage;
import com.oocourse.library3.annotation.Trigger;
import static com.oocourse.library3.LibraryIO.PRINTER;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Comparator;
import java.util.stream.Collectors;

public class LibraryController {
    private LocalDate currentDate;
    private final Map<LibraryBookId, BookCopy> allBookCopies;
    private final Map<LibraryBookIsbn, List<BookCopy>> booksByIsbn;
    private final Map<String, LibUser> allUsers;
    private final List<OrderRequest> pendingOrderRequests;
    private final Map<BookCopy, OrderRequest> booksAtAppointmentOffice;
    private final Set<LibraryBookIsbn> hotIsbnsFromLastOpenDay;
    private final Set<LibraryBookIsbn> newlyHotIsbnsThisDay;
    private final Map<BookCopy, LibUser> booksInReadingRoom;
    private int bookNum1;
    private int bookNum2;
    private int bookNum3;
    private int bookNum4;
    private int hotNum1;
    private int hotNum2;
    private int hotNum3;
    private int hotNum4;
    private int appointmentNum1;
    private int appointmentNum2;

    public LibraryController() {
        this.allBookCopies = new HashMap<>();
        this.booksByIsbn = new HashMap<>();
        this.allUsers = new HashMap<>();
        this.pendingOrderRequests = new ArrayList<>();
        this.booksAtAppointmentOffice = new HashMap<>();
        this.hotIsbnsFromLastOpenDay = new HashSet<>();
        this.newlyHotIsbnsThisDay = new HashSet<>();
        this.booksInReadingRoom = new HashMap<>();
    }

    public void setCurrentDate(LocalDate date) {
        this.currentDate = date;
    }

    public void initializeInventory(Map<LibraryBookIsbn, Integer> inventory) {
        for (Map.Entry<LibraryBookIsbn, Integer> entry : inventory.entrySet()) {
            LibraryBookIsbn isbn = entry.getKey();
            int count = entry.getValue();
            booksByIsbn.putIfAbsent(isbn, new ArrayList<>());
            for (int i = 1; i <= count; i++) {
                //副本号从1开始依递增顺序编号, e.g., "01", "02"
                String copyIdStr = String.format("%02d", i);
                LibraryBookId bookId = new LibraryBookId(isbn.getType(), isbn.getUid(), copyIdStr);
                BookCopy bookCopy = new BookCopy(bookId);
                allBookCopies.put(bookId, bookCopy);
                booksByIsbn.get(isbn).add(bookCopy);
            }
        }
    }

    private List<BookCopy> getAvailableCopiesOnShelf(LibraryBookIsbn isbn) {
        return booksByIsbn.getOrDefault(isbn, new ArrayList<>())
                .stream()
                .filter(b -> b.getCurrentLocation() == LibraryBookState.BOOKSHELF || // 普通书架
                        b.getCurrentLocation() == LibraryBookState.HOT_BOOKSHELF) // 热门书架
                .sorted(Comparator.comparing(b -> b.getId().getCopyId()))
                .collect(Collectors.toList());
    }

    private LibUser getOrCreateUser(String studentId) {
        return allUsers.computeIfAbsent(studentId, LibUser::new);
    }

    private void organizeBooks(boolean isOpeningSort) {
        List<LibraryMoveInfo> moves = new ArrayList<>();
        handleOverdueAppointments(moves, isOpeningSort);
        if (isOpeningSort) {
            handleReadingRoomClearanceForOpening(moves);
            handleBorrowReturnOfficeClearance(moves);
            handleHotNormalShelfSorting(moves);
        } else {
            handleBorrowReturnOfficeClearance(moves);
        }
        handlePendingOrderRequests(moves, isOpeningSort);
        PRINTER.move(currentDate, moves);
    }

    @Trigger(from = "APPOINTMENT_OFFICE", to = "BOOKSHELF")
    private void handleOverdueAppointments(List<LibraryMoveInfo> moves, boolean isOpeningSort) {
        List<BookCopy> booksToRemoveFromAo = new ArrayList<>();
        // 遍历副本以安全移除
        for (Map.Entry<BookCopy, OrderRequest> entry :
            new HashMap<>(booksAtAppointmentOffice).entrySet()) {
            BookCopy book = entry.getKey();
            OrderRequest order = entry.getValue();
            if (order.isExpiredBy(currentDate, isOpeningSort)) {
                moves.add(new LibraryMoveInfo(book.getId(),
                    LibraryBookState.APPOINTMENT_OFFICE, LibraryBookState.BOOKSHELF));
                book.moveTo(LibraryBookState.BOOKSHELF, currentDate);
                book.setReservationInfo(null);
                booksToRemoveFromAo.add(book);
                LibUser user = getOrCreateUser(order.getStudentId());
                if (user.getActiveOrderRequest() == order) {
                    user.setActiveOrderRequest(null);
                }
            }
        }
        booksToRemoveFromAo.forEach(booksAtAppointmentOffice::remove);
    }

    @Trigger(from = "READING_ROOM", to = "BORROW_RETURN_OFFICE")
    private void handleReadingRoomClearanceForOpening(List<LibraryMoveInfo> moves) {
        List<BookCopy> booksToRemoveFromRr = new ArrayList<>();
        for (Map.Entry<BookCopy, LibUser> entry : new HashMap<>(booksInReadingRoom).entrySet()) {
            BookCopy bookInRr = entry.getKey();
            LibUser reader = entry.getValue();
            moves.add(new LibraryMoveInfo(bookInRr.getId(),
                LibraryBookState.READING_ROOM, LibraryBookState.BORROW_RETURN_OFFICE));
            bookInRr.moveTo(LibraryBookState.BORROW_RETURN_OFFICE, currentDate);
            if (reader.getCurrentReadingBook() == bookInRr) {
                reader.setCurrentReadingBook(null, null);
            }
            booksToRemoveFromRr.add(bookInRr);
        }
        booksToRemoveFromRr.forEach(booksInReadingRoom::remove);
    }

    @Trigger(from = "BORROW_RETURN_OFFICE", to = "BOOKSHELF")
    private void handleBorrowReturnOfficeClearance(List<LibraryMoveInfo> moves) {
        // 借还处的书统一先移到普通书架
        for (BookCopy book : allBookCopies.values()) {
            if (book.getCurrentLocation() == LibraryBookState.BORROW_RETURN_OFFICE) {
                moves.add(new LibraryMoveInfo(book.getId(),
                    LibraryBookState.BORROW_RETURN_OFFICE, LibraryBookState.BOOKSHELF));
                book.moveTo(LibraryBookState.BOOKSHELF, currentDate);
            }
        }
    }

    @Trigger(from = "BOOKSHELF", to = "HOT_BOOKSHELF")
    @Trigger(from = "HOT_BOOKSHELF", to = "BOOKSHELF")
    private void handleHotNormalShelfSorting(List<LibraryMoveInfo> moves) {
        Set<LibraryBookIsbn> currentHotIsbns = this.hotIsbnsFromLastOpenDay;
        for (BookCopy book : allBookCopies.values()) {
            LibraryBookState currentLocation = book.getCurrentLocation();
            if (currentLocation == LibraryBookState.BOOKSHELF ||
                currentLocation == LibraryBookState.HOT_BOOKSHELF) {
                boolean shouldBeHot = currentHotIsbns.contains(book.getIsbn());
                if (shouldBeHot && currentLocation == LibraryBookState.BOOKSHELF) {
                    moves.add(new LibraryMoveInfo(book.getId(),
                        LibraryBookState.BOOKSHELF, LibraryBookState.HOT_BOOKSHELF));
                    book.moveTo(LibraryBookState.HOT_BOOKSHELF, currentDate);
                } else if (!shouldBeHot && currentLocation == LibraryBookState.HOT_BOOKSHELF) {
                    moves.add(new LibraryMoveInfo(book.getId(),
                        LibraryBookState.HOT_BOOKSHELF, LibraryBookState.BOOKSHELF));
                    book.moveTo(LibraryBookState.BOOKSHELF, currentDate);
                }
            }
        }
    }

    @Trigger(from = "BOOKSHELF", to = "APPOINTMENT_OFFICE")
    @Trigger(from = "HOT_BOOKSHELF", to = "APPOINTMENT_OFFICE")
    private void handlePendingOrderRequests(List<LibraryMoveInfo> moves, boolean isOpeningSort) {
        List<OrderRequest> fulfilledOrders = new ArrayList<>();
        pendingOrderRequests.sort(Comparator.comparing(OrderRequest::getOrderDate)
            .thenComparing(OrderRequest::getStudentId)); // 确保排序稳定性
        for (OrderRequest order : pendingOrderRequests) {
            List<BookCopy> availableCopies = getAvailableCopiesOnShelf(order.getBookIsbn());
            if (!availableCopies.isEmpty()) {
                BookCopy bookToReserve = availableCopies.get(0);
                LibraryBookState fromShelf = bookToReserve.getCurrentLocation();
                moves.add(new LibraryMoveInfo(bookToReserve.getId(), fromShelf,
                    LibraryBookState.APPOINTMENT_OFFICE, order.getStudentId()));
                bookToReserve.moveTo(LibraryBookState.APPOINTMENT_OFFICE, currentDate);
                bookToReserve.setReservationInfo(order);
                order.setAssignedBookCopy(bookToReserve);
                order.setBookReadyAtAoDate(currentDate);

                LocalDate expiryDate;
                if (isOpeningSort) {
                    expiryDate = currentDate.plusDays(4);
                } else {
                    expiryDate = currentDate.plusDays(5);
                }
                order.setExpiryDateAtAo(expiryDate);
                booksAtAppointmentOffice.put(bookToReserve, order);
                fulfilledOrders.add(order);
            }
        }
        pendingOrderRequests.removeAll(fulfilledOrders);
    }

    public void handleQueryCreditScore(LibraryQcsCmd cmd) {
        LibUser user = getOrCreateUser(cmd.getStudentId());
        PRINTER.info(cmd, user.getCreditScore());
    }

    public void handleOpen(LibraryOpenCmd cmd) {
        this.newlyHotIsbnsThisDay.clear();
        organizeBooks(true);
    }

    public void handleClose(LibraryCloseCmd cmd) {
        // 1. 处理当日阅读不还的扣分 (这是与当天操作相关的)
        for (LibUser user : allUsers.values()) {
            if (user.getCurrentReadingBook() != null
                && user.hasUnreturnedReadBookOnDate(currentDate)) {
                user.deductCredit(10);
            }
        }

        // 2. 调用通用的每日罚款结算逻辑
        processDailyPenalties(currentDate);

        // 3. 执行闭馆整理
        organizeBooks(false);
        this.hotIsbnsFromLastOpenDay.clear();
        this.hotIsbnsFromLastOpenDay.addAll(this.newlyHotIsbnsThisDay);
    }

    public void handleQuery(LibraryReqCmd cmd) {
        LibraryBookId bookIdToQuery = cmd.getBookId(); // Query is by specific copy
        BookCopy book = allBookCopies.get(bookIdToQuery);
        if (book != null) {
            PRINTER.info(currentDate, bookIdToQuery, book.getMovementHistory());
        } else {
            // Should not happen based on "输入指令中操作的书籍编号，一定在图书馆中存在"
            // But as a fallback, print empty history or handle as error
            PRINTER.info(currentDate, bookIdToQuery, new ArrayList<>());
        }
    }

    @Trigger(from = "BOOKSHELF", to = "USER")
    @Trigger(from = "HOT_BOOKSHELF", to = "USER")
    public void handleBorrow(LibraryReqCmd cmd) {
        String studentId = cmd.getStudentId();
        LibraryBookIsbn isbnToBorrow = cmd.getBookIsbn(); // Borrow is by ISBN
        LibUser user = getOrCreateUser(studentId);

        if (!user.canBorrow()) {
            PRINTER.reject(cmd);
            return;
        }

        // 1. A类书不可借阅
        if (isbnToBorrow.isTypeA()) {
            PRINTER.reject(cmd);
            return;
        }

        // 2. Check user's borrowing limits for B and C books
        if (!user.canSatisfyBorrowingLimitFor(isbnToBorrow)) {
            PRINTER.reject(cmd);
            return;
        }

        // 3. Check if book has copies on shelf
        List<BookCopy> availableCopies = getAvailableCopiesOnShelf(isbnToBorrow);
        if (availableCopies.isEmpty()) {
            PRINTER.reject(cmd); // "若欲借阅的书籍无余本在架，借阅失败"
            return;
        }

        // Success
        BookCopy borrowedBook = availableCopies.get(0); // Pick one, e.g., first by copyId
        borrowedBook.moveTo(LibraryBookState.USER, currentDate);
        borrowedBook.setCurrentHolder(user);
        borrowedBook.setBorrowDate(currentDate);
        user.addBook(borrowedBook);

        PRINTER.accept(cmd, borrowedBook.getId());
        this.newlyHotIsbnsThisDay.add(borrowedBook.getIsbn());
    }

    @Trigger(from = "USER", to = "BORROW_RETURN_OFFICE")
    public void handleReturn(LibraryReqCmd cmd) {
        LibUser user = getOrCreateUser(cmd.getStudentId());
        BookCopy book = allBookCopies.get(cmd.getBookId());
        if (book == null || book.getCurrentLocation() != LibraryBookState.USER
            || book.getCurrentHolder() != user) {
            PRINTER.reject(cmd);
            return;
        }

        LocalDate borrowDate = book.getBorrowDate();
        boolean isOverdue = false;
        if (borrowDate != null) {
            int loanDays = book.getIsbn().isTypeB() ? 30 : 60;
            LocalDate dueDate = borrowDate.plusDays(loanDays);
            if (currentDate.isAfter(dueDate)) {
                isOverdue = true;
            }
        }

        String additionalInfo;
        if (isOverdue) {
            additionalInfo = "overdue";
        } else {
            user.addCredit(10);
            additionalInfo = "not overdue";
        }

        user.removeBook(book);
        book.moveTo(LibraryBookState.BORROW_RETURN_OFFICE, currentDate);
        book.clearBorrowDate();

        PRINTER.accept(cmd, additionalInfo);
    }

    @SendMessage(from = "MainClass", to = "LibraryController")
    @SendMessage(from = "LibraryController", to = "LibUser")
    @SendMessage(from = "LibraryController", to = "MainClass")
    public void orderNewBook(LibraryReqCmd cmd) {
        String studentId = cmd.getStudentId();
        LibraryBookIsbn isbnToOrder = cmd.getBookIsbn();
        LibUser user = getOrCreateUser(studentId);

        if (!user.canOrder()) {
            PRINTER.reject(cmd);
            return;
        }

        if (isbnToOrder.isTypeA()) {
            PRINTER.reject(cmd);
            return;
        }
        if (user.getActiveOrderRequest() != null) {
            PRINTER.reject(cmd);
            return;
        }
        if (!user.canSatisfyOrderingLimitFor(isbnToOrder)) {
            PRINTER.reject(cmd);
            return;
        }
        OrderRequest newOrder = new OrderRequest(studentId, isbnToOrder, currentDate);
        user.setActiveOrderRequest(newOrder);
        pendingOrderRequests.add(newOrder);
        PRINTER.accept(cmd);
    }

    @Trigger(from = "APPOINTMENT_OFFICE", to = "USER")
    @SendMessage(from = "MainClass", to = "LibraryController")
    @SendMessage(from = "LibraryController", to = "MainClass")
    public void getOrderedBook(LibraryReqCmd cmd) {
        String studentId = cmd.getStudentId();
        LibraryBookIsbn isbnToPick = cmd.getBookIsbn();
        LibUser user = getOrCreateUser(studentId);

        if (!user.canBorrow()) {
            PRINTER.reject(cmd);
            return;
        }

        OrderRequest activeOrder = user.getActiveOrderRequest();
        if (activeOrder == null || !activeOrder.isAssigned()
            || !activeOrder.getBookIsbn().equals(isbnToPick)) {
            PRINTER.reject(cmd);
            return;
        }
        BookCopy reservedBook = activeOrder.getAssignedBookCopy();
        if (reservedBook == null ||
            reservedBook.getCurrentLocation() != LibraryBookState.APPOINTMENT_OFFICE ||
            !booksAtAppointmentOffice.getOrDefault(reservedBook, null).equals(activeOrder)) {
            PRINTER.reject(cmd);
            return;
        }
        if (activeOrder.isExpiredBy(currentDate, true)) {
            PRINTER.reject(cmd); // Reservation expired
            return;
        }

        // 4. Check if picking up violates borrowing limits
        if (!user.canSatisfyBorrowingLimitFor(reservedBook.getIsbn())) {
            PRINTER.reject(cmd);
            return;
        }

        // Success
        booksAtAppointmentOffice.remove(reservedBook);
        reservedBook.moveTo(LibraryBookState.USER, currentDate);
        reservedBook.setCurrentHolder(user);
        reservedBook.setBorrowDate(currentDate);

        user.addBook(reservedBook);
        user.setActiveOrderRequest(null); // Order fulfilled

        PRINTER.accept(cmd, reservedBook.getId());
    }

    @Trigger(from = "BOOKSHELF", to = "READING_ROOM")
    @Trigger(from = "HOT_BOOKSHELF", to = "READING_ROOM")
    public void handleRead(LibraryReqCmd cmd) {
        String studentId = cmd.getStudentId();
        LibraryBookIsbn isbnToRead = cmd.getBookIsbn();
        LibUser user = getOrCreateUser(studentId);

        if (!user.canRead(isbnToRead.getType())) {
            PRINTER.reject(cmd);
            return;
        }

        // 1. 检查用户当日是否已阅读未归还
        if (user.hasUnreturnedReadBookOnDate(currentDate)) {
            PRINTER.reject(cmd);
            return;
        }

        // 2. 查找可阅读书籍 (普通书架或热门书架)
        List<BookCopy> availableCopies = getAvailableCopiesOnShelf(isbnToRead);
        if (availableCopies.isEmpty()) {
            PRINTER.reject(cmd); // 无余本在架
            return;
        }

        // 3. A类书可以阅读 (所以不在此处拒绝A类)

        // 成功阅读
        BookCopy bookToRead = availableCopies.get(0); // 取副本号最小的
        LibraryBookState fromLocation = bookToRead.getCurrentLocation(); // 记录原始书架

        bookToRead.moveTo(LibraryBookState.READING_ROOM, currentDate);
        user.setCurrentReadingBook(bookToRead, currentDate); // 记录用户正在阅读
        this.booksInReadingRoom.put(bookToRead, user);   // 记录书在阅览室被谁阅读

        this.newlyHotIsbnsThisDay.add(bookToRead.getIsbn()); // 阅读也使书变热门

        PRINTER.accept(cmd, bookToRead.getId());
    }

    @Trigger(from = "READING_ROOM", to = "BORROW_RETURN_OFFICE")
    public void handleRestore(LibraryReqCmd cmd) {
        String studentId = cmd.getStudentId();
        // 归还指令使用具体书号
        LibraryBookId bookIdToRestore = cmd.getBookId();
        LibUser user = getOrCreateUser(studentId);
        BookCopy book = allBookCopies.get(bookIdToRestore);
        if (book != null && book.getCurrentLocation() == LibraryBookState.READING_ROOM &&
            this.booksInReadingRoom.getOrDefault(book, null) == user &&
            user.getCurrentReadingBook() == book) {

            this.booksInReadingRoom.remove(book);
            user.setCurrentReadingBook(null, null); // 清除用户阅读状态
            book.moveTo(LibraryBookState.BORROW_RETURN_OFFICE, currentDate);
            user.addCredit(10);
            PRINTER.accept(cmd);
        } else {
            if (book != null && book.getCurrentLocation() == LibraryBookState.READING_ROOM) {
                LibUser reader = this.booksInReadingRoom.remove(book);
                if (reader != null && reader.getCurrentReadingBook() == book) {
                    reader.setCurrentReadingBook(null, null);
                }
                book.moveTo(LibraryBookState.BORROW_RETURN_OFFICE, currentDate);
                PRINTER.accept(cmd);
            } else {
                PRINTER.reject(cmd);
            }
        }
    }

    public void processDailyPenalties(LocalDate date) {
        // 1. 逾期还书扣分
        for (LibUser user : allUsers.values()) {
            for (BookCopy book : user.getHeldBooks()) {
                if (book.getBorrowDate() != null) {
                    int loanDays = book.getIsbn().isTypeB() ? 30 : 60;
                    LocalDate dueDate = book.getBorrowDate().plusDays(loanDays);

                    if (!date.isBefore(dueDate)) {
                        user.deductCredit(5);
                    }
                }
            }
        }

        // 2. 预约不取扣分 (此部分逻辑是正确的，无需修改)
        for (OrderRequest order : booksAtAppointmentOffice.values()) {
            if (order.getExpiryDateAtAo() != null && order.isExpiredBy(date, false)) {
                LibUser user = getOrCreateUser(order.getStudentId());
                user.deductCredit(15);
            }
        }
    }
}