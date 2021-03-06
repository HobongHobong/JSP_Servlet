package com.java.servlet.pds;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.java.dto.AttachVO;
import com.java.dto.PdsVO;
import com.java.service.PdsService;
import com.java.service.PdsServiceImpl;
import com.java.utils.GetUploadPath;
import com.java.utils.MakeFileName;

@WebServlet("/pds/regist")
public class RegistPdsServlet extends HttpServlet {

	private PdsService service = PdsServiceImpl.getInstance();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String url = "/WEB-INF/views/pds/regist.jsp";

		request.getRequestDispatcher(url).forward(request, response);
	}

	// 업로드 파일 환경 설정
	private static final int MEMORY_THRESHOLD = 1024 * 1024 * 3; // 3MB
	private static final int MAX_FILE_SIZE = 1024 * 1024 * 40; // 40MB
	private static final int MAX_REQUEST_SIZE = 1024 * 1024 * 50; // 50MB

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// request 파일 첨부 여부 확인.
		if (!ServletFileUpload.isMultipartContent(request)) {
			PrintWriter out = response.getWriter();
			out.println("<script>");
			out.println("alert('multipart/form-data 형식이 아닙니다.')");
			out.println("</script>");
			out.flush();
			out.close();
			return;
		}

		// 업로드를 위한 upload 환경설정 적용.
		DiskFileItemFactory factory = new DiskFileItemFactory();
		// 저장을 위한 threshold memory 적용.
		factory.setSizeThreshold(MEMORY_THRESHOLD);
		// 임시 저장 위치 결정.
		factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
		ServletFileUpload upload = new ServletFileUpload(factory);

		// 업로드 파일의 크기 적용.
		upload.setFileSizeMax(MAX_FILE_SIZE);

		// 업로드 request 크기 적용.
		upload.setSizeMax(MAX_REQUEST_SIZE);

		// 실제 저장 경로를 설정.
		// String uploadPath = getServletContext().getRealPath("") + UPLOAD_DIRECTORY;
		String uploadPath = GetUploadPath.getUploadPath("pds.upload");

		
		File file = new File(uploadPath);
		if (!file.mkdirs()) {
			System.out.println(uploadPath+" 생성을 실패했습니다.");
		}
		

		// request로 부터 받는 파일을 추출해서 저장.
		try {
			List<FileItem> formItems = upload.parseRequest(request);

			// 파일 개수 확인.
			if (formItems != null && formItems.size() > 0) {
				List<AttachVO> attachList = new ArrayList<AttachVO>();
				String writer = null; // request.getParameter("writer")
				String title = null; // request.getParameter("title");
				String content = null;// request.getParameter("content");
				int pno = -1; // Integer.parseInt(request.getParameter("pno"))
				for (FileItem item : formItems) {
					if (!item.isFormField()) {
						String fileName = new File(item.getName()).getName();
						fileName = MakeFileName.toUUIDFileName(fileName, "$$");
						String filePath = uploadPath + File.separator + fileName;
						File storeFile = new File(filePath);

						// local HDD 에 저장.
						item.write(storeFile);

						// DB에 저장할 attach에 file 내용 추가.
						AttachVO attach = new AttachVO();
						attach.setFileName(fileName);
						attach.setUploadPath(uploadPath);
						attach.setFileType(fileName.substring(fileName.lastIndexOf(".") + 1));

						attachList.add(attach);

						System.out.println("upload file : " + attach);

						request.setAttribute("message", "업로드 되었습니다.");
					} else {
						switch (item.getFieldName()) {
						case "title":
							title = item.getString("utf-8");
							break;
						case "writer":
							writer = item.getString("utf-8");
							break;
						case "content":
							content = item.getString("utf-8");
							break;
						case "pno":
							pno = Integer.parseInt(item.getString("utf-8"));
							break;
						}
					}
				}
				PdsVO pds=new PdsVO();
				pds.setAttachList(attachList);
				pds.setWriter(writer);
				pds.setContent(content);
				pds.setTitle(title);
				
				service.regist(pds);
			}
		} catch (Exception e) {
			e.printStackTrace();
			request.setAttribute("message", "파일 업로드에 실패했습니다.");
		}

		String url = "list";
		response.sendRedirect(url);

	}
}
