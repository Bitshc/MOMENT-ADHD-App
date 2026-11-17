import pathlib
import re
import sqlite3
import unittest

SOURCE=(pathlib.Path(__file__).parents[1]/'MomentAndroid/app/src/main/java/com/moment/app/Store.java').read_text(encoding='utf-8')

class TaskDatabaseTest(unittest.TestCase):
    def setUp(self):
        self.db=sqlite3.connect(':memory:')
        for table in ('items','requests','event_details','steps','task_details'):
            sql=re.search(r'd.execSQL\("(CREATE TABLE (?:IF NOT EXISTS )?'+table+r'\([^"]+)"',SOURCE).group(1)
            self.db.execute(sql)
        self.db.execute("INSERT INTO items(id,title,kind,day) VALUES(1,'Laundry','chore','2026-11-16')")
        self.db.execute("INSERT INTO items(id,title,kind,day) VALUES(2,'Study','task','2026-11-17')")
        self.db.execute("INSERT INTO items(id,title,kind,day) VALUES(3,'Dentist','event','2026-11-17')")

    def query(self,method):
        return re.search(r'public Cursor '+method+r'\(\).*?rawQuery\("([^"]+)"',SOURCE).group(1)

    def test_new_details_table_preserves_existing_items(self):
        self.assertEqual(self.db.execute('SELECT COUNT(*) FROM items').fetchone()[0],3)
        self.assertEqual(self.db.execute(self.query('taskReminders')).fetchall(),[])

    def test_only_opted_in_unfinished_timed_tasks_notify(self):
        self.db.execute("INSERT INTO task_details(item_id,time,remind,repeat) VALUES(1,'09:00',1,'weekly')")
        self.db.execute("INSERT INTO task_details(item_id,time,remind) VALUES(2,'10:00',0)")
        rows=self.db.execute(self.query('taskReminders')).fetchall()
        self.assertEqual([r[0] for r in rows],[1])
        self.db.execute('UPDATE items SET done=1 WHERE id=1')
        self.assertEqual(self.db.execute(self.query('taskReminders')).fetchall(),[])

    def test_timed_chore_is_in_calendar_with_its_own_category(self):
        self.db.execute("INSERT INTO task_details(item_id,time,duration_minutes) VALUES(1,'09:15',45)")
        rows=self.db.execute(self.query('calendarItems')).fetchall()
        row=next(r for r in rows if r[0]==1)
        self.assertEqual(row[5:8],('09:15',45,'Chore'))

    def test_snooze_only_applies_to_current_unfinished_occurrence(self):
        self.db.execute("INSERT INTO task_details(item_id,time,remind) VALUES(1,'09:00',1)")
        sql=re.search(r'execSQL\("(UPDATE task_details SET snooze_until=[^"]+)"',SOURCE).group(1)
        self.db.execute(sql,(12345,1,'2026-11-15'))
        self.assertEqual(self.db.execute('SELECT snooze_until FROM task_details').fetchone()[0],0)
        self.db.execute(sql,(12345,1,'2026-11-16'))
        self.assertEqual(self.db.execute('SELECT snooze_until FROM task_details').fetchone()[0],12345)
        self.db.execute('UPDATE items SET done=1 WHERE id=1')
        self.db.execute(sql,(67890,1,'2026-11-16'))
        self.assertEqual(self.db.execute('SELECT snooze_until FROM task_details').fetchone()[0],12345)

if __name__=='__main__': unittest.main()
