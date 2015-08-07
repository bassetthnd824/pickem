   SELECT M.MATCHUP_ID
        , S.SEASON_ID
        , S.SEASON
        , SW.SEASON_WEEK_ID
        , SW.WEEK_NUMBER
        , SW.BEGIN_DATE AS WEEK_BEGIN_DATE
        , SW.END_DATE AS WEEK_END_DATE
        , M.MATCHUP_DATE
        , M.AWAY_TEAM_ID
        , T2.TEAM_NAME AS AWAY_TEAM_NAME
        , T2.SQUAD_NAME AS AWAY_SQUAD_NAME
        , M.AWAY_TEAM_SCORE
        , M.HOME_TEAM_ID
        , T1.TEAM_NAME AS HOME_TEAM_NAME
        , T1.SQUAD_NAME AS HOME_SQUAD_NAME
        , M.HOME_TEAM_SCORE
        , V.VENUE_NAME
        , V.CITY_STATE
        , R.RIVALRY_NAME
        , UP.USER_PICK_ID
        , U.USER_GUID
        , U.USER_ID
        , UP.PICKED_TEAM_ID
        , UP.RANK
     FROM PCKM_MATCHUP       M
     JOIN PCKM_SEASON_WEEK   SW
       ON M.SEASON_WEEK_ID = SW.SEASON_WEEK_ID
     JOIN PCKM_SEASON        S
       ON SW.SEASON_ID     = S.SEASON_ID
     JOIN PCKM_TEAM          T1
       ON M.HOME_TEAM_ID   = T1.TEAM_ID
     JOIN PCKM_TEAM          T2
       ON M.AWAY_TEAM_ID   = T2.TEAM_ID
     JOIN PCKM_VENUE         V
       ON M.VENUE_ID       = V.VENUE_ID
LEFT JOIN PCKM_RIVALRY       R
       ON(M.HOME_TEAM_ID   = R.TEAM_ID1
      AND M.AWAY_TEAM_ID   = R.TEAM_ID2)
       OR(M.HOME_TEAM_ID   = R.TEAM_ID2
      AND M.AWAY_TEAM_ID   = R.TEAM_ID1)
LEFT JOIN PCKM_USER_PICK     UP
       ON M.MATCHUP_ID     = UP.MATCHUP_ID
      AND UP.USER_GUID     = 1
LEFT JOIN PCKM_USER          U
       ON UP.USER_GUID     = U.USER_GUID
    WHERE S.SEASON_ID      = 3
--      AND(U.USER_GUID      = 1
--       OR U.USER_GUID     IS NULL)
 ORDER BY S.SEASON
        , SW.WEEK_NUMBER
        , UP.RANK        DESC nulls last
        , M.MATCHUP_DATE
        , T1.TEAM_NAME;
        
        
values next value for PCKM_USER_PICK_SEQ;