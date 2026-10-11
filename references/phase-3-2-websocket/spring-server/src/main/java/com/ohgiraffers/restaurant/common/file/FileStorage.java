package com.ohgiraffers.restaurant.common.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/* 설명. 업로드 파일을 프로젝트 바깥의 폴더에 저장하고 삭제하는 부품
 *  04_spring_boot chap06(file upload)의 FileUploadUtils와 같은 방식이다.
 *  ----------------------------------------------------------------------------------
 *  업로드 파일을 프로젝트 안(build 폴더 등)에 저장하면 다시 빌드할 때 지워지거나,
 *  소스 코드와 사용자 데이터가 한곳에 섞여 Git에 올라가는 문제가 생긴다.
 *  그래서 OS별로 지정한 외부 경로(application.yaml의 file.upload.*)에 저장한다.
 *  ----------------------------------------------------------------------------------
 *  저장된 파일은 WebConfig의 설정 덕분에 http://localhost:8080/uploads/** 주소로 접근할 수 있다.
 * */
@Component
public class FileStorage {

    @Value("${file.upload.windows.path}")
    private String windowsPath;

    @Value("${file.upload.mac.path}")
    private String macPath;

    /* 설명. 현재 OS에 맞는 업로드 기본 경로를 반환한다. */
    public String getBasePath() {

        String osName = System.getProperty("os.name").toLowerCase();

        return osName.contains("win") ? windowsPath : macPath;
    }

    /**
     * 파일을 하위 폴더에 저장하고, 저장된 파일명을 반환한다.
     *
     * <p>사용자가 올린 파일명은 겹칠 수 있으므로 UUID로 새 파일명을 만들어 저장한다.
     * DB에는 이 '저장된 파일명'만 기록한다.</p>
     *
     * @param file 업로드된 파일
     * @param subDirectory 저장할 하위 폴더명 (예: menus)
     * @return 저장된 파일명 (예: 3f2a...c1.png)
     */
    public String save(MultipartFile file, String subDirectory) {

        File directory = new File(getBasePath() + "/" + subDirectory);

        // 폴더가 없으면 만든다.
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("파일 업로드 폴더 생성 실패: " + directory.getAbsolutePath());
        }

        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName != null && originalFileName.contains(".")
                ? originalFileName.substring(originalFileName.lastIndexOf("."))
                : "";
        String savedName = UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            file.transferTo(new File(directory, savedName));
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장 실패: " + originalFileName, e);
        }

        return savedName;
    }

    /* 설명. 저장된 파일을 삭제한다. 파일이 없으면 아무 일도 하지 않는다. */
    public void delete(String savedName, String subDirectory) {

        if (savedName == null) {
            return;
        }

        File file = new File(getBasePath() + "/" + subDirectory, savedName);

        if (file.exists() && !file.delete()) {
            System.out.println("파일 삭제 실패: " + file.getAbsolutePath());
        }
    }
}
