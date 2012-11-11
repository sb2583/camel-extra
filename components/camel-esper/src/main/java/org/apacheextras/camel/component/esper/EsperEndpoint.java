/**************************************************************************************
 http://code.google.com/a/apache-extras.org/p/camel-extra

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.


 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 02110-1301, USA.

 http://www.gnu.org/licenses/gpl-2.0-standalone.html
 ***************************************************************************************/
package org.apacheextras.camel.component.esper;

import java.util.concurrent.atomic.AtomicInteger;

import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.util.ObjectHelper;

/**
 * An endpoint for working with <a href="http//esper.codehaus.org/">Esper</a>
 *
 * @version $Revision: 1.1 $
 */
public class EsperEndpoint extends DefaultEndpoint {
    private EsperComponent component;
    private String name;
    private boolean mapEvents;
    private String pattern;
    private String eql;
    private EPStatement statement;
    private AtomicInteger consumers = new AtomicInteger(0);

    public EsperEndpoint(String uri, EsperComponent component, String name) {
        super(uri, component);
        this.component = component;
        this.name = name;
    }

    public boolean isSingleton() {
        return true;
    }

    public EsperProducer createProducer() throws Exception {
        return new EsperProducer(this);
    }

    public EsperConsumer createConsumer(Processor processor) throws Exception {
        EPStatement stat = getStatement();
        consumers.incrementAndGet();
        return new EsperConsumer(this, statement, processor);
    }

    @Override
    public PollingConsumer createPollingConsumer() throws Exception {
        EPStatement stat = getStatement();
        consumers.incrementAndGet();
        return new EsperPollingConsumer(this, statement);
    }

    private EPStatement getStatement() {
        if (statement == null) {
            statement = createStatement();
            //statement.start();
        }
        return statement;
    }

    protected EPStatement createStatement() {
        if (pattern != null) {
            return getEsperAdministrator().createPattern(pattern);
        }
        else {
            ObjectHelper.notNull(eql, "eql or pattern");
            return getEsperAdministrator().createEPL(eql);
        }
    }

    public synchronized void removeConsumer() {
        if (0 == consumers.decrementAndGet()) {
            statement.stop();
            statement.destroy();
        }
    }

    /**
     * Creates a Camel {@link Exchange} from an Esper {@link EventBean} instance
     */
    public Exchange createExchange(EventBean eventBean, EPStatement statement) {
        Exchange exchange = createExchange(ExchangePattern.InOnly);
        Message in = exchange.getIn();
        in.setHeader("CamelEsperName", name);
        in.setHeader("CamelEsperStatement", statement);
        if (pattern != null) {
            in.setHeader("CamelEsperPattern", pattern);
        }
        if (eql != null) {
            in.setHeader("CamelEsperEql", eql);
        }
        in.setBody(eventBean);
        return exchange;
    }

    // Properties
    //-------------------------------------------------------------------------
    public String getName() {
        return name;
    }

    public EPRuntime getEsperRuntime() {
        return component.getEsperRuntime();
    }

    public EPServiceProvider getEsperService() {
        return component.getEsperService();
    }

    public EPAdministrator getEsperAdministrator() {
        return getEsperService().getEPAdministrator();
    }

    public boolean isMapEvents() {
        return mapEvents;
    }

    /**
     * Should we use Map events (the default approach) containing all the message headers
     * and the message body in the "body" entry, or should we just send the body of the message
     * as the event.
     *
     * @param mapEvents whether or not we should send map events.
     */
    public void setMapEvents(boolean mapEvents) {
        this.mapEvents = mapEvents;
    }

    public String getEql() {
        return eql;
    }

    /**
     * Sets the EQL statement used for consumers
     */
    public void setEql(String eql) {
        this.eql = eql;
    }

    public String getPattern() {
        return pattern;
    }

    /**
     * Sets the Esper pattern used for consumers
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}