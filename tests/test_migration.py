import pathlib
import re
import sqlite3
import unittest

SOURCE = (pathlib.Path(__file__).parents[1] / "MomentAndroid/app/src/main/java/com/moment/app/Store.java").read_text(encoding="utf-8")


class MigrationTest(unittest.TestCase):
    def test_existing_version_three_data_survives(self):
        db = sqlite3.connect(":memory:")
        db.executescript("""
            CREATE TABLE items(id INTEGER PRIMARY KEY,title TEXT,kind TEXT,day TEXT,done INTEGER);
            CREATE TABLE event_details(item_id INTEGER PRIMARY KEY,time TEXT NOT NULL DEFAULT '',address TEXT NOT NULL DEFAULT '',trip_minutes INTEGER NOT NULL DEFAULT 0);
            CREATE TABLE steps(id INTEGER PRIMARY KEY,item_id INTEGER,title TEXT,minutes INTEGER,done INTEGER);
            CREATE TABLE requests(id INTEGER PRIMARY KEY,sender TEXT,title TEXT,day TEXT,status TEXT);
            INSERT INTO items VALUES(1,'Doctor','event','2026-11-12',0);
            INSERT INTO items VALUES(2,'Laundry','task','2026-11-12',1);
            INSERT INTO event_details VALUES(1,'14:00','123 Main St',20);
            INSERT INTO steps VALUES(3,2,'Fold',5,1);
            INSERT INTO requests VALUES(4,'Partner','Dinner','2026-11-12','pending');
        """)
        migration = re.search(r'd.execSQL\("(ALTER TABLE event_details [^"]+)"\)', SOURCE).group(1)
        db.execute(migration)
        self.assertEqual(db.execute("SELECT * FROM event_details").fetchone(), (1, '14:00', '123 Main St', 20, 0))
        self.assertEqual(db.execute("SELECT done FROM items WHERE id=2").fetchone(), (1,))
        self.assertEqual(db.execute("SELECT done FROM steps WHERE id=3").fetchone(), (1,))
        self.assertEqual(db.execute("SELECT status FROM requests WHERE id=4").fetchone(), ('pending',))
        db.execute("UPDATE event_details SET remind=1 WHERE item_id=1")
        query = re.search(r'public Cursor reminderEvents\(\).*?rawQuery\("([^"]+)"', SOURCE).group(1)
        self.assertEqual(db.execute(query).fetchall(), [(1, 'Doctor', '2026-11-12', '14:00', '123 Main St')])
        db.execute("DELETE FROM items WHERE id=1")
        self.assertEqual(db.execute(query).fetchall(), [])

    def test_new_event_reminders_start_opted_out(self):
        db = sqlite3.connect(":memory:")
        schema = re.search(r'd.execSQL\("(CREATE TABLE IF NOT EXISTS event_details[^"]+)"\)', SOURCE).group(1)
        db.execute(schema)
        db.execute("INSERT INTO event_details(item_id,time) VALUES(1,'12:00')")
        self.assertEqual(db.execute("SELECT remind FROM event_details").fetchone(), (0,))


if __name__ == '__main__':
    unittest.main()
