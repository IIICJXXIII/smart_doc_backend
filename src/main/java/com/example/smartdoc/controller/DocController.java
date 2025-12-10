package com.example.smartdoc.controller;

import com.example.smartdoc.model.InvoiceData;
import com.example.smartdoc.model.User;
import com.example.smartdoc.repository.InvoiceRepository;
import com.example.smartdoc.service.OcrService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/doc")
@CrossOrigin(origins = "*")
public class DocController {

    @Autowired
    private OcrService ocrService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private HttpServletRequest request; // 注入 request 以获取 Header

    // 1. 上传识别 (不需要改，识别不涉及存库)
    @PostMapping("/upload")
    public InvoiceData uploadAndAnalyze(@RequestParam("file") MultipartFile file) {
        try {
            return ocrService.processDocument(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 2. 保存归档 (Create) - 绑定当前用户
    @PostMapping("/save")
    public String saveDoc(@RequestBody InvoiceData data) {
        // A. 获取当前登录用户
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "error: not login";
        }

        // B. 绑定 UserID
        data.setUserId(currentUser.getId());

        invoiceRepository.save(data);
        return "success";
    }

    // 3. 获取列表 (Read) - 只查自己的数据
    @GetMapping("/list")
    public List<InvoiceData> getList() {
        // A. 获取当前登录用户
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return List.of(); // 未登录返回空列表
        }

        // B. 调用 Repository 新写的方法，只查这个人的
        return invoiceRepository.findByUserIdOrderByIdDesc(currentUser.getId());
    }

    // 4. 删除 (Delete) - 安全校验
    @DeleteMapping("/delete/{id}")
    public String deleteDoc(@PathVariable Long id) {
        User currentUser = getCurrentUser();

        // 查一下这条数据是不是存在的
        InvoiceData data = invoiceRepository.findById(id).orElse(null);

        // 只有数据存在，且属于当前用户，才允许删除
        if (data != null && data.getUserId().equals(currentUser.getId())) {
            invoiceRepository.deleteById(id);
            return "success";
        } else {
            return "fail: permission denied"; // 没权限删别人的
        }
    }

    /**
     * 辅助方法：从 Header 的 Token 中获取当前用户对象
     */
    private User getCurrentUser() {
        String token = request.getHeader("Authorization");
        if (token != null && UserController.tokenMap.containsKey(token)) {
            return UserController.tokenMap.get(token);
        }
        return null; // Token 无效或未登录
    }
}