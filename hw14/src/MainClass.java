import com.oocourse.library2.LibraryBookIsbn;
import com.oocourse.library2.LibraryCommand;
import com.oocourse.library2.LibraryCloseCmd;
import com.oocourse.library2.LibraryOpenCmd;
import com.oocourse.library2.LibraryReqCmd;
import static com.oocourse.library2.LibraryIO.SCANNER;
import java.util.Map;

public class MainClass {
    public static void main(String[] args) {
        LibraryController libraryController = new LibraryController();
        // 获取图书馆初始书目信息
        Map<LibraryBookIsbn, Integer> inventory = SCANNER.getInventory();
        libraryController.initializeInventory(inventory);

        while (true) {
            LibraryCommand command = SCANNER.nextCommand(); // 读取下一条指令
            if (command == null) {
                break; // 输入结束
            }

            libraryController.setCurrentDate(command.getDate()); // 更新当前日期

            if (command instanceof LibraryOpenCmd) {
                libraryController.handleOpen((LibraryOpenCmd) command);
            } else if (command instanceof LibraryCloseCmd) {
                libraryController.handleClose((LibraryCloseCmd) command);
            } else if (command instanceof LibraryReqCmd) {
                LibraryReqCmd req = (LibraryReqCmd) command;
                // 根据请求类型分发处理
                switch (req.getType()) {
                    case QUERIED:
                        libraryController.handleQuery(req);
                        break;
                    case BORROWED:
                        libraryController.handleBorrow(req);
                        break;
                    case ORDERED:
                        libraryController.handleOrder(req);
                        break;
                    case RETURNED:
                        libraryController.handleReturn(req);
                        break;
                    case PICKED:
                        libraryController.handlePick(req);
                        break;
                    case READ:
                        libraryController.handleRead(req);
                        break;
                    case RESTORED:
                        libraryController.handleRestore(req);
                        break;
                    default:
                        // 根据题目描述，不应出现其他类型
                        break;
                }
            }
            // 确保每次读入指令后都有一次输出，由各handle方法内部的PRINTER调用完成
        }
    }
}