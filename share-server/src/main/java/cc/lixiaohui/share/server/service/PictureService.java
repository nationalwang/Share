package cc.lixiaohui.share.server.service;

import java.util.Map;

import cc.lixiaohui.share.server.core.Session;
import cc.lixiaohui.share.server.core.SystemRuntime;
import cc.lixiaohui.share.server.model.bean.Picture;
import cc.lixiaohui.share.server.model.dao.PictureDao;
import cc.lixiaohui.share.server.model.dao.util.DaoException;
import cc.lixiaohui.share.server.service.util.PrivilegeLevel;
import cc.lixiaohui.share.server.service.util.ServiceException;
import cc.lixiaohui.share.server.service.util.annotation.Procedure;
import cc.lixiaohui.share.server.service.util.annotation.Service;
import cc.lixiaohui.share.util.ArrayUtils;
import cc.lixiaohui.share.util.ErrorCode;
import cc.lixiaohui.share.util.FileUtils;
import cc.lixiaohui.share.util.JSONUtils;
import cc.lixiaohui.share.util.UUIDUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 图片相关操作实现
 * @author lixiaohui
 * @date 2016年11月7日 下午9:50:47
 */
@Service(name = "PictureService")
public class PictureService extends AbstractService{
	
	public PictureService(Session session, Map<String, Object> parameters) {
		super(session, parameters);
	}
	
	/**
	 * 上传图片
	 * <pre>
	 * int userId, String suffix, byte[] bytes
	 * {
	 *     "id":2
	 * }
	 * </pre>
	 * @return
	 * @throws ServiceException
	 */
	@Procedure(name = "uploadPicture", level=PrivilegeLevel.LOGGED)
	public String uploadPicture() throws ServiceException {
		
		if (!session.isLogined()) {
			return JSONUtils.newFailureResult("您未登录", ErrorCode.AUTH, "");
		}
		
		String suffix = null;
		byte[] bytes = null;
		
		try {
			suffix = getStringParameter("suffix", "");
			bytes = getObjectParameter("bytes");
		} catch (Throwable t) {
			logger.error("{}", t);
			return JSONUtils.newFailureResult(t.getMessage(), ErrorCode.PARAMETER, t);
		}
		
		String fullPath = null;
		boolean fileSaved = false;
		boolean success = false;
		try {
			// 1.保存图片字节至硬盘
			PictureDao dao = daofactory.getDao(PictureDao.class);
			fullPath = FileUtils.saveFile(SystemRuntime.picturePath(), generatePictureName(dao, suffix), bytes);
			fileSaved = true;
			// 2.保存记录到库
			Picture picture = newPicture(session.getUser().getId(), suffix, fullPath);
			
			if (dao.add(picture) > 0) { // 保存成功
				JSONObject result = new JSONObject();
				result.put("id", picture.getId());
				success = true;
				return JSONUtils.newSuccessfulResult("上传成功", result);
			} else {
				return JSONUtils.newFailureResult("上传失败", ErrorCode.UNKOWN, "");
			}
		} catch (Exception e) {
			logger.error("{}", e);
			return JSONUtils.newFailureResult(e.getMessage(), ErrorCode.wrap(e), e);
		} finally {
			if (!success && fileSaved) {
				// 回滚刚保存的文件
				if (FileUtils.deleteFile(fullPath)) {
					logger.debug("deleted file {}", fullPath);
				} else {
					logger.debug("unable to delete file {}", fullPath);
				}
			}
		}
	}

	/**
	 * 获取图片信息
	 * <pre>
	 * boolean ignoreIfNotExist, int[] pictureIds
	 * {
	 *       "count":2,
	 *       "pictures":
	 *         [
	 *           {
	 *             "id":32,                    # 图片ID
	 *             "bytes":dshdssad,           # 图片字节
	 *             "uploadTime":14678787787,   # 图片上传时间
	 *             "uploadUserId":321,         # 上传该图片的用户ID
	 *           },
	 *           {
	 *             "id":33,                    # 图片ID
	 *             "bytes":kjdajkjjk,          # 图片字节
	 *             "uploadTime":14678787787,   # 图片上传时间
	 *             "uploadUserId":321,         # 上传该图片的用户ID
	 *           }
	 *         ]
	 *     }
	 * </pre>
	 * @return
	 * @throws ServiceException
	 */
	@Procedure(name = "getPictures")
	public String getPictures() throws ServiceException {
		//boolean ignoreIfNotExist;
		int[] pictureIds = null;
		try {
			//ignoreIfNotExist = getBooleanParameter("ignoreIfNotExist", true);
			pictureIds = getObjectParameter("pictureIds");
		} catch (Throwable e) {
			logger.error("参数错误");
			return JSONUtils.newFailureResult(e.getMessage(), ErrorCode.PARAMETER, "");
		}
		
		try {
			PictureDao dao = daofactory.getDao(PictureDao.class);
			JSONArray array = new JSONArray();
			// 遍历获取图片字节内容
			for (int pictureId : pictureIds) {
				try {
					Picture picture = dao.getById(pictureId);
					if (picture == null) {
						array.add(emptyPictureObject(pictureId));
						continue;
					} 
					
					byte[] content = FileUtils.getResourceAsBytes(picture.getPath());
					if (content == null) {
						array.add(emptyPictureObject(pictureId));
						continue;
					}
					
					JSONObject o = new JSONObject();
					o.put("id", picture.getId());
					o.put("bytes", content);
					o.put("uploadTime", picture.getUploadTime());
					o.put("uploadUserId", picture.getUploadUserId());
					array.add(o);
				} catch (Exception e1) {
					logger.error("{}", e1);
					array.add(emptyPictureObject(pictureId));
					continue;
				}
			}
			JSONObject result = new JSONObject();
			result.put("count", array.size());
			result.put("pictures", array);
			return JSONUtils.newSuccessfulResult("获取图片成功", result);
		} catch (Exception e) {
			logger.error("{}", e);
			return JSONUtils.newFailureResult("获取图片出错", ErrorCode.wrap(e), e);
		}
	}
	
	/**
	 * 删除图片
	 * <pre>
	 * int pictureId
	 * {}
	 * </pre>
	 * @return
	 * @throws ServiceException
	 */
	@Procedure(name = "deletePicture", level=PrivilegeLevel.LOGGED)
	public String deletePicture() throws ServiceException {
		int pictureId;
		try {
			pictureId = getIntParameter("pictureId");
		} catch (Throwable t) {
			return JSONUtils.newFailureResult(t.getMessage(), ErrorCode.PARAMETER, t);
		}
		
		try {
			PictureDao dao = daofactory.getDao(PictureDao.class);
			// 获取图片记录
			Picture picture = dao.getById(pictureId);
			// 删除图片文件
			try {
				FileUtils.deleteFile(picture.getPath());
			} catch (Exception e1){
				logger.error("picture not found {}", picture.getPath());
			}
			// 删除图片记录
			if (dao.delete(picture) > 0) {
				return JSONUtils.newSuccessfulResult("删除成功");
			} else {
				return JSONUtils.newFailureResult("删除失败", ErrorCode.UNKOWN, "");
			}
		} catch (Exception e) {
			logger.error("{}", e);
			return JSONUtils.newFailureResult("删除错误", ErrorCode.wrap(e), e);
		}
	}
	
	private Picture newPicture(int userId, String suffix, String fullPath) {
		Picture picture = new Picture();
		picture.setPath(fullPath);
		picture.setSuffix(suffix == null ? "" : suffix);
		picture.setUploadUserId(userId);
		return picture;
	}
	
	private JSONObject emptyPictureObject(int id) {
		JSONObject p = new JSONObject();
		p.put("id", id);
		p.put("bytes", ArrayUtils.EMPTY_BYTE_ARRAY);
		p.put("uploadTime", 0);
		p.put("uploadUserId", 0);
		return p;
	}
	
	private synchronized String generatePictureName(PictureDao dao, String suffix) throws DaoException {
		String p = UUIDUtils.random32Byte();
		return (suffix == null || suffix.equals("")) ? p : p + "." + suffix;
	}
}
