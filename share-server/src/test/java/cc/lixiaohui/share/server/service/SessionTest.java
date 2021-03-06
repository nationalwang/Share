package cc.lixiaohui.share.server.service;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Test;

import cc.lixiaohui.share.server.model.bean.User;
import cc.lixiaohui.share.server.model.util.HibernateSessionFactory;

/**
 * @author lixiaohui
 * @date 2016年11月14日 上午9:44:20
 */
public class SessionTest {
	
	SessionFactory factory = HibernateSessionFactory.getSessionFactory();
	
	@Test
	public void test() {
		Session session = factory.openSession();
		
		User user = (User) session.get(User.class, 6);
		session.close();
		
		Session s1 = factory.openSession();
		Transaction trans = s1.beginTransaction();
		user.setSignature("哈哈哈哈哈哈哈");
		s1.update(user);
		s1.update(user);
		trans.commit();
		s1.close();
		
	}
	
}
