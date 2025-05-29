# -*- coding: utf-8 -*-
import requests
import pymysql
from pymysql import cursors
import time
import random
import logging
from typing import List, Dict
from bs4 import BeautifulSoup
import re

# 配置日志系统
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('book_spider.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# 数据库配置
DB_CONFIG = {
    'host': '192.168.56.10',
    'port': 3306,
    'database': 'tomatomall',
    'user': 'root',
    'password': '123456',
    'charset': 'utf8mb4',
    'cursorclass': cursors.DictCursor  # 添加字典游标
}
# ['编程', '小说', '哲学', '科技', '历史', '心理学', '经济', '管理', '艺术', 
#             '科幻', '漫画', '旅行', '教育', '童书', '政治', '传记', '医学', '文化',
#             '社会科学', '设计', '建筑', '摄影']
# 豆瓣网页配置
DOUBAN_CONFIG = {
    'base_url': 'https://book.douban.com/tag/', # 标签页URL
    'tags': ['小说', '哲学', '科技', '历史', '心理学', '经济', '管理', '艺术', 
             '科幻', '漫画', '旅行', '教育', '童书', '政治', '传记', '医学', '文化',
             '社会科学', '设计', '建筑', '摄影'],
    'max_page': 1,
    'delay': 1.0  # 基础延迟
}

def get_book_detail(session, detail_url: str) -> str:
    """获取图书详情页的内容简介"""
    try:
        # 添加随机延迟避免频繁请求
        time.sleep(random.uniform(0.1, 1.0))
        
        logger.info(f"正在获取图书详情: {detail_url}")
        response = session.get(detail_url)
        response.raise_for_status()
        
        soup = BeautifulSoup(response.text, 'html.parser')
        
        # 查找内容简介
        intro_element = soup.select_one('#link-report .intro')
        if not intro_element:
            # 尝试其他可能的选择器
            intro_element = soup.select_one('div.related_info div.intro')
        
        # 提取文本内容
        if intro_element:
            # 移除<p>标签但保留内容并添加换行
            paragraphs = intro_element.find_all('p')
            if paragraphs:
                intro_text = '\n'.join([p.get_text(strip=True) for p in paragraphs])
            else:
                intro_text = intro_element.get_text(strip=True)
            # logger.info(f"intro_text: {intro_text}")
            return intro_text
        
        return "暂无内容简介"
    except Exception as e:
        logger.error(f"获取图书详情时出错: {str(e)}")
        return "获取内容简介失败"

def extract_book_data(session, book_element) -> Dict:
    """从网页元素中提取图书数据"""
    try:
        # 提取标题
        title_element = book_element.select_one('div.info h2 a')
        title = title_element.get_text(strip=True) if title_element else '未知书名'
        
        # 提取详情页链接
        detail_url = title_element.get('href', '') if title_element else ''
        
        # 提取评分
        rate_element = book_element.select_one('span.rating_nums')
        rate = float(rate_element.get_text(strip=True)) if rate_element and rate_element.get_text(strip=True) else 0.0
        
        # 提取描述摘要
        desc_element = book_element.select_one('p')
        short_desc = desc_element.get_text(strip=True) if desc_element else '暂无描述'
        
        # 提取图书信息（作者、出版社等）
        info_element = book_element.select_one('div.pub')
        info_text = info_element.get_text(strip=True) if info_element else ''
        info_parts = [part.strip() for part in info_text.split('/') if part.strip()]
        
        # 提取封面图片
        cover_element = book_element.select_one('div.pic img')
        cover = cover_element.get('src', '') if cover_element else ''
        
        # 处理价格信息 (从信息字符串中尝试提取)
        price = 0.0
        if len(info_parts) >= 1:
            price_match = re.search(r'(\d+(\.\d+)?)', info_parts[-1])
            if price_match:
                try:
                    price = float(price_match.group(1))
                except ValueError:
                    price = 0.0
        
        # 构建规格信息
        specs = {}
        if len(info_parts) >= 4:  # 通常格式为：作者/出版社/出版日期/价格
            specs['作者'] = info_parts[0]
            specs['出版社'] = info_parts[1] if len(info_parts) > 1 else '未知出版社'
            specs['出版日期'] = info_parts[2] if len(info_parts) > 2 else '未知日期'
            specs['价格'] = info_parts[-1] if len(info_parts) > 3 else '未知价格'
            
        # 获取内容简介 (访问详情页)
        detail_content = "暂无内容简介"
        if detail_url:
            detail_content = get_book_detail(session, detail_url)
        
        return {
            'title': title,
            'price': price,
            'rate': rate,
            'description': short_desc[:255] if short_desc else "暂无描述",
            'cover': cover,
            'detail': detail_content[:2000] if detail_content else "暂无内容简介",  # 存储内容简介
            'specs': specs
        }
    except Exception as e:
        logger.error(f"解析图书元素时出错: {str(e)}")
        return None

def save_to_database(db_conn, cursor, book_data: Dict) -> bool:
    """保存到MySQL数据库"""
    logger.debug(f"尝试保存图书到数据库: {book_data['title']}")
    try:
        # 使用pymysql的上下文管理
        with db_conn.cursor() as cursor:
            # 插入products表
            product_sql = """
            INSERT INTO products 
            (title, price, rate, description, cover, detail)
            VALUES (%(title)s, %(price)s, %(rate)s, %(description)s, %(cover)s, %(detail)s)
            """
            cursor.execute(product_sql, {
                'title': book_data['title'],
                'price': book_data['price'],
                'rate': book_data['rate'],
                'description': book_data['description'],
                'cover': book_data['cover'],
                'detail': book_data['detail']
            })
            product_id = cursor.lastrowid
            
            # 批量插入specifications
            spec_sql = "INSERT INTO specifications (item, value, product_id) VALUES (%s, %s, %s)"
            specs = [(k, str(v), product_id) for k, v in book_data['specs'].items() if v]
            if specs:
                cursor.executemany(spec_sql, specs)
            
            # 插入库存记录
            stock_sql = "INSERT INTO stockpiles (product_id, amount, frozen, locked_amount) VALUES (%s, %s, %s, %s)"
            cursor.execute(stock_sql, (product_id, random.randint(50, 200), 0, 0))
            
        db_conn.commit()  # 统一提交事务
        logger.debug(f"成功保存图书: {book_data['title']}")
        return True
    except pymysql.Error as err:
        logger.error(f"数据库错误: {err}")
        db_conn.rollback()
        return False

def fetch_books_by_tag(session, tag: str) -> List[Dict]:
    """通过标签获取豆瓣图书数据"""
    logger.info(f"开始爬取 [{tag}] 分类图书...")
    books = []
    success_pages = 0
    retry_count = 0
    max_retries = 3
    
    # 对中文标签进行URL编码
    encoded_tag = requests.utils.quote(tag)
    base_url = f"{DOUBAN_CONFIG['base_url']}{encoded_tag}"
    
    for page in range(DOUBAN_CONFIG['max_page']):
        try:
            # 豆瓣标签页的分页URL格式
            if page > 0:
                url = f"{base_url}?start={page * 20}&type=T"
            else:
                url = base_url
                
            logger.info(f"正在处理 [{tag}] 第 {page+1}/{DOUBAN_CONFIG['max_page']} 页: {url}")
            
            # 使用随机延迟避免被封
            sleep_time = DOUBAN_CONFIG['delay'] * (1 + random.uniform(0, 0.5))
            time.sleep(sleep_time)
            
            # 发送请求获取页面内容
            response = session.get(url)
            response.raise_for_status()
            
            # 解析HTML
            soup = BeautifulSoup(response.text, 'html.parser')
            
            # 获取图书列表
            book_elements = soup.select('li.subject-item')
            
            if not book_elements:
                logger.warning(f"[{tag}] 第 {page+1} 页没有找到图书元素，可能已到达末页或格式变化")
                break
            
            page_books = []
            for book_element in book_elements:
                book_data = extract_book_data(session, book_element)
                if book_data:
                    books.append(book_data)
                    page_books.append(book_data['title'])
            
            logger.info(f"成功获取 [{tag}] 第 {page+1} 页数据，找到 {len(page_books)} 本图书")
            if page_books:
                logger.debug(f"本页部分书籍: {', '.join(page_books[:3])}{'...' if len(page_books) > 3 else ''}")
            
            success_pages += 1
            retry_count = 0  # 重置重试计数
            
        except requests.exceptions.HTTPError as e:
            status_code = e.response.status_code
            if status_code == 404:
                logger.warning(f"[{tag}] 分类页面不存在，跳过此分类")
                break
            elif status_code == 429 or status_code == 403:
                retry_delay = 60 + random.uniform(0, 30)  # 对封禁/限流使用更长的等待时间
                logger.warning(f"请求被限制 (状态码: {status_code})，暂停 {retry_delay:.2f} 秒后重试...")
                time.sleep(retry_delay)
                retry_count += 1
                if retry_count <= max_retries:
                    logger.info(f"第 {retry_count}/{max_retries} 次重试...")
                    page -= 1  # 重试当前页
                else:
                    logger.error(f"重试次数超过上限，跳过当前页")
                    retry_count = 0
                continue
            else:
                logger.error(f"HTTP错误: {e}，跳过当前页")
                continue
        except requests.exceptions.ConnectionError as e:
            retry_delay = 15 + random.uniform(0, 10)
            logger.warning(f"连接错误: {e}，等待 {retry_delay:.2f} 秒后重试...")
            time.sleep(retry_delay)
            retry_count += 1
            if retry_count <= max_retries:
                page -= 1  # 重试当前页
            else:
                logger.error(f"重试次数超过上限，跳过当前页")
                retry_count = 0
            continue
        except Exception as e:
            logger.error(f"获取 {tag} 第 {page+1} 页时出错: {str(e)}")
            continue
            
    logger.info(f"[{tag}] 分类爬取完成，共处理 {success_pages} 页，获取 {len(books)} 本图书")
    return books

def main():
    """主程序"""
    logger.info("====== 豆瓣图书爬虫启动 ======")
    start_time = time.time()
    total_books = 0
    total_categories = 0
    
    try:
        # 初始化数据库连接
        logger.info("正在连接到数据库...")
        db_conn = pymysql.connect(**DB_CONFIG)
        cursor = db_conn.cursor()
        logger.info("数据库连接成功")
        
        # 初始化会话
        session = requests.Session()
        session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
            'Cookie': ''  # 可以添加Cookie提高稳定性
        })
        
        logger.info(f"计划爬取 {len(DOUBAN_CONFIG['tags'])} 个分类")
        
        for tag in DOUBAN_CONFIG['tags']:
            category_start = time.time()
            logger.info(f"开始处理分类: {tag}")
            
            books = fetch_books_by_tag(session, tag)
            
            if books:
                logger.info(f"开始将 [{tag}] 分类的 {len(books)} 本图书保存到数据库...")
                success_count = 0
                
                for i, book in enumerate(books):
                    if i % 10 == 0:
                        logger.info(f"保存 [{tag}] 分类进度: {i}/{len(books)}")
                    
                    if save_to_database(db_conn, cursor, book):
                        success_count += 1
                
                category_time = time.time() - category_start
                total_books += success_count
                total_categories += 1
                logger.info(f"[{tag}] 分类完成，成功入库 {success_count}/{len(books)} 本，耗时 {category_time:.2f} 秒")
            else:
                logger.warning(f"[{tag}] 分类未获取到图书，跳过")
            
            logger.info("---------------------------")
            
        total_time = time.time() - start_time
        logger.info("\n====== 爬虫任务完成 ======")
        logger.info(f"共处理 {total_categories}/{len(DOUBAN_CONFIG['tags'])} 个分类")
        logger.info(f"总计入库 {total_books} 本图书")
        logger.info(f"总耗时: {total_time/60:.2f} 分钟")
        
    except KeyboardInterrupt:
        logger.warning("\n用户中断，爬虫停止")
    except Exception as e:
        logger.error(f"爬虫发生严重错误: {str(e)}", exc_info=True)
    finally:
        if 'cursor' in locals() and cursor:
            cursor.close()
        if 'db_conn' in locals() and db_conn:
            db_conn.close()
            logger.info("数据库连接已关闭")

if __name__ == "__main__":
    main()