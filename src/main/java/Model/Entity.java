package Model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lớp cơ sở trừu tượng cho tất cả entity trong hệ thống.
 * Cung cấp trường id chung và implements Serializable để truyền qua socket.
 * 
 * Các lớp kế thừa: {@link User}, {@link Item}.
 */
public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;
    
    protected String id;
    protected LocalDateTime createdAt;
    
    /**
     * @param id mã định danh duy nhất
     */
    public Entity(String id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
    }
    
    /** @return mã định danh duy nhất */
    public String getId() {
        return id;
    }
    
    /** @param id mã định danh mới */
    public void setId(String id) {
        this.id = id;
    }
    
    /** @return thời điểm tạo entity */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Lấy thông tin chi tiết đặc thù của entity.
     * Mỗi lớp con triển khai để trả về chuỗi mô tả riêng.
     *
     * @return chuỗi thông tin chi tiết
     */
    public abstract String getSpecificInfo();
    
    @Override
    public String toString() {
        return String.format("[%s] %s", getClass().getSimpleName(), id);
    }
}
