CREATE TABLE IF NOT EXISTS movies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    genre VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    rating VARCHAR(20),
    description TEXT,
    poster_url VARCHAR(1000),
    trailer_url VARCHAR(1000),
    showtimes TEXT
);

DELETE FROM movies;

INSERT INTO movies
(id, title, genre, status, rating, description, poster_url, trailer_url, showtimes)
VALUES
(
    1,
    'Dune: Part Two',
    'Sci-Fi',
    'CURRENTLY_RUNNING',
    'PG-13',
    'Paul Atreides unites with the Fremen while seeking revenge against those who destroyed his family.',
    'https://placehold.co/300x450?text=Dune+Part+Two',
    'https://www.youtube.com/embed/Way9Dexny3w',
    '["2:00 PM", "5:00 PM", "8:00 PM"]'
),
(
    2,
    'Inside Out 2',
    'Animation',
    'CURRENTLY_RUNNING',
    'PG',
    'A young girl navigates new emotions as she grows older.',
    'https://placehold.co/300x450?text=Inside+Out+2',
    'https://www.youtube.com/embed/LEjhY15eCx0',
    '["11:00 AM", "1:30 PM", "4:00 PM"]'
),
(
    3,
    'The Dark Knight',
    'Action',
    'CURRENTLY_RUNNING',
    'PG-13',
    'Batman faces the Joker, a criminal mastermind who brings chaos to Gotham City.',
    'https://placehold.co/300x450?text=The+Dark+Knight',
    'https://www.youtube.com/embed/EXeTwQWrcwY',
    '["12:30 PM", "3:45 PM", "7:15 PM"]'
),
(
    4,
    'La La Land',
    'Musical',
    'CURRENTLY_RUNNING',
    'PG-13',
    'A pianist and an actress fall in love while pursuing their dreams in Los Angeles.',
    'https://placehold.co/300x450?text=La+La+Land',
    'https://www.youtube.com/embed/0pdqf4P9MB8',
    '["10:30 AM", "2:15 PM", "6:00 PM"]'
),
(
    5,
    'A Quiet Place',
    'Horror',
    'CURRENTLY_RUNNING',
    'PG-13',
    'A family must survive in silence while hiding from creatures that hunt by sound.',
    'https://placehold.co/300x450?text=A+Quiet+Place',
    'https://www.youtube.com/embed/WR7cc5t7tv8',
    '["5:30 PM", "8:45 PM", "10:30 PM"]'
),
(
    6,
    'Oppenheimer',
    'Drama',
    'CURRENTLY_RUNNING',
    'R',
    'A physicist leads a secret project that changes the course of history.',
    'https://placehold.co/300x450?text=Oppenheimer',
    'https://www.youtube.com/embed/uYPbbksJxIg',
    '["1:15 PM", "4:45 PM", "8:15 PM"]'
),
(
    7,
    'Barbie',
    'Comedy',
    'CURRENTLY_RUNNING',
    'PG-13',
    'Barbie leaves her perfect world and begins a journey of self-discovery.',
    'https://placehold.co/300x450?text=Barbie',
    'https://www.youtube.com/embed/pBk4NYhWNMM',
    '["12:00 PM", "3:00 PM", "6:30 PM"]'
),
(
    8,
    'Spider-Man: Across the Spider-Verse',
    'Animation',
    'CURRENTLY_RUNNING',
    'PG',
    'Miles Morales travels across the multiverse and meets other Spider-People.',
    'https://placehold.co/300x450?text=Spider+Verse',
    'https://www.youtube.com/embed/shW9i6k8cB0',
    '["11:45 AM", "2:45 PM", "7:30 PM"]'
),
(
    9,
    'Avatar 3',
    'Sci-Fi',
    'COMING_SOON',
    'PG-13',
    'The story of Pandora continues with new conflicts and discoveries.',
    'https://placehold.co/300x450?text=Avatar+3',
    'https://www.youtube.com/embed/d9MyW72ELq0',
    '["2:00 PM", "5:00 PM", "8:00 PM"]'
),
(
    10,
    'Toy Story 5',
    'Animation',
    'COMING_SOON',
    'PG',
    'The toys return for a new adventure with old friends and new challenges.',
    'https://placehold.co/300x450?text=Toy+Story+5',
    'https://www.youtube.com/embed/wmiIUN-7qhE',
    '["10:00 AM", "1:00 PM", "4:00 PM"]'
),
(
    11,
    'Mission: Impossible',
    'Action',
    'COMING_SOON',
    'PG-13',
    'Ethan Hunt returns for another dangerous mission.',
    'https://placehold.co/300x450?text=Mission+Impossible',
    'https://www.youtube.com/embed/avz06PDqDbM',
    '["12:15 PM", "3:30 PM", "7:45 PM"]'
),
(
    12,
    'Paddington 3',
    'Comedy',
    'COMING_SOON',
    'PG',
    'Paddington begins another heartwarming journey with his family.',
    'https://placehold.co/300x450?text=Paddington+3',
    'https://www.youtube.com/embed/52x5HJ9H8DM',
    '["11:30 AM", "2:30 PM", "5:30 PM"]'
),
(
    13,
    'The Conjuring: Last Rites',
    'Horror',
    'COMING_SOON',
    'R',
    'A new paranormal case challenges investigators in a terrifying final chapter.',
    'https://placehold.co/300x450?text=The+Conjuring',
    'https://www.youtube.com/embed/ejMMn0t58Lc',
    '["6:15 PM", "8:45 PM", "10:45 PM"]'
),
(
    14,
    'Interstellar',
    'Sci-Fi',
    'COMING_SOON',
    'PG-13',
    'A group of astronauts travels through a wormhole in search of a new home for humanity.',
    'https://placehold.co/300x450?text=Interstellar',
    'https://www.youtube.com/embed/zSWdZVtXT7E',
    '["1:00 PM", "4:30 PM", "8:00 PM"]'
),
(
    15,
    'The Super Mario Bros. Movie',
    'Animation',
    'COMING_SOON',
    'PG',
    'Mario and Luigi enter a colorful new world and join a battle to save the Mushroom Kingdom.',
    'https://placehold.co/300x450?text=Super+Mario+Bros',
    'https://www.youtube.com/embed/TnGl01FkMMo',
    '["10:45 AM", "1:45 PM", "4:45 PM"]'
);
