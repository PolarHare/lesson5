package com.polarnick.rss;

import com.google.common.base.Preconditions;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Date: 13.10.13
 *
 * @author Nickolay Polyarniy aka PolarNick
 */
public class StackOverflowRSSReader {

    private static final String URL = "http://stackoverflow.com/feeds/tag/android";
    private static final String ENCODING_CHARSET = "UTF-8";

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private final XmlPullParser parser;

    public StackOverflowRSSReader() throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        parser = factory.newPullParser();//About this exception at least javadocs says some information(but it is still very strange)
    }

    public Feed readFeed() throws IOException, XmlPullParserException, ParseException {
        HttpGet getRequest = new HttpGet(URL);
        HttpResponse httpResponse = new DefaultHttpClient().execute(getRequest);
        parser.setInput(httpResponse.getEntity().getContent(), ENCODING_CHARSET);

        Preconditions.checkState(parser.getEventType() == XmlPullParser.START_DOCUMENT);
        Preconditions.checkState(parser.next() == XmlPullParser.START_TAG && "feed".equals(parser.getName()));
        Feed feed = readFeed(parser);
        Preconditions.checkState(parser.next() == XmlPullParser.END_DOCUMENT);
        return feed;
    }

    private Feed readFeed(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        Preconditions.checkState(parser.getEventType() == XmlPullParser.START_TAG && "feed".equals(parser.getName()));

        Feed feed = new Feed();
        int eventType = parser.next();
        int level = 0;
        while (!(eventType == XmlPullParser.END_TAG && level == 0)) {
            if (eventType == XmlPullParser.START_TAG) {
                level++;
            }
            if (eventType == XmlPullParser.START_TAG && level == 1 && "entry".equals(parser.getName())) {
                FeedEntry entry = readEntry(parser);
                feed.addEntry(entry);
            }
            if (parser.getEventType() == XmlPullParser.END_TAG) {
                level--;
            }
            eventType = parser.next();
        }
        Preconditions.checkState(parser.getEventType() == XmlPullParser.END_TAG && "feed".equals(parser.getName()));
        return feed;
    }

    private FeedEntry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        Preconditions.checkState(parser.getEventType() == XmlPullParser.START_TAG && "entry".equals(parser.getName()));

        FeedEntry entry = new FeedEntry();
        int eventType = parser.next();
        int level = 0;
        ArrayList<String> tagsStack = new ArrayList<String>();
        while (!(eventType == XmlPullParser.END_TAG && level == 0)) {
            if (eventType == XmlPullParser.START_TAG) {
                if (tagsStack.size() > level) {
                    tagsStack.set(level, parser.getName());
                } else {
                    Preconditions.checkState(tagsStack.size() == level);
                    tagsStack.add(parser.getName());
                }
                level++;
            }
            if (eventType == XmlPullParser.START_TAG && level == 1 && "link".equals(parser.getName())) {
                entry.setLink(parser.getAttributeValue(null, "href"));
            } else if (eventType == XmlPullParser.TEXT && level == 1 && "title".equals(tagsStack.get(level - 1))) {
                entry.setTitle(parser.getText());
            } else if (eventType == XmlPullParser.TEXT && level == 1 && "published".equals(tagsStack.get(level - 1))) {
                entry.setPublishedDate(dateFormat.parse(parser.getText()));
            } else if (eventType == XmlPullParser.TEXT && level == 1 && "updated".equals(tagsStack.get(level - 1))) {
                entry.setUpdatedDate(dateFormat.parse(parser.getText()));
            } else if (eventType == XmlPullParser.TEXT && level == 1 && "re:rank".equals(tagsStack.get(level - 1))) {
                entry.setRank(Integer.parseInt(parser.getText()));
            }
            if (eventType == XmlPullParser.END_TAG) {
                tagsStack.set(level - 1, null);
                level--;
            }
            eventType = parser.next();
        }
        Preconditions.checkState(parser.getEventType() == XmlPullParser.END_TAG && "entry".equals(parser.getName()));
        return entry;
    }

}
