package cc.lixiaohui.share.server.model.dao;

import java.util.List;

import cc.lixiaohui.share.server.model.bean.Share;
import cc.lixiaohui.share.server.model.dao.util.DaoException;

/**
 * @author lixiaohui
 * @date 2016年10月30日 下午5:00:06
 */
public interface ShareDao extends DeleteableDao<Share> {
	
	public Share getByIdForDetail(int shareId) throws DaoException;
	
	public Share getByIdForComment(int shareId) throws DaoException;
	
	public List<Share> list(int currentUserId, int targetUserId, String keyword, long baseTime, boolean deleted, String orderColumnName, String orderTypeName, int start, int limit) throws DaoException;
	
	public List<Object[]> listLikedByUser(int userId, int start, int limit) throws DaoException;
	
	public List<Object[]> listCommentedByUser(int userId, int start, int limit) throws DaoException;
	
}
