<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="jakarta.tags.core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:master title="Tietosuojaseloste - Ryyppy.net">
    <jsp:attribute name="customHead">
        <link rel="stylesheet" type="text/css" href="/static/css/style.css" />
        <link rel="stylesheet" type="text/css" href="/static/css/login.css" />
        <style>
            .privacy-policy {
                background-color: #000000;
                border: 3px solid #505050;
                border-radius: 10px;
                margin: 20px auto;
                padding: 20px 30px;
                color: #ffffff;
                max-width: 800px;
                text-align: left;
                line-height: 1.6;
            }
            .privacy-policy h1 {
                text-align: center;
                margin-bottom: 30px;
                font-size: 1.8em;
            }
            .privacy-policy h2 {
                margin-top: 25px;
                margin-bottom: 15px;
                font-size: 1.3em;
                color: #ffffff;
                border-bottom: 1px solid #505050;
                padding-bottom: 5px;
            }
            .privacy-policy p, .privacy-policy li {
                color: #cccccc;
                margin-bottom: 10px;
            }
            .privacy-policy ul {
                margin-left: 20px;
                margin-bottom: 15px;
            }
            .privacy-policy a {
                color: #aaaaaa;
                text-decoration: underline;
            }
            .privacy-policy a:hover {
                color: #ffffff;
            }
            .back-link {
                text-align: center;
                margin: 20px;
            }
            .back-link a {
                color: #aaaaaa;
                text-decoration: none;
            }
            .last-updated {
                text-align: center;
                color: #888888;
                font-size: 0.9em;
                margin-top: 30px;
            }
        </style>
    </jsp:attribute>

    <jsp:body>
        <div id="logoContainer">
            <a href="/ui/login">
                <img id="logo" src="/static/images/logo_ryyppy.png" alt="Ryyppy.net" title="Ryyppy.net" style="max-width: 300px;" />
            </a>
        </div>

        <div class="privacy-policy">
            <h1>Tietosuojaseloste</h1>

            <h2>1. Rekisterinpitäjä</h2>
            <p>
                Ryyppy.net<br>
                Sähköposti: privacy@ryyppy.net
            </p>

            <h2>2. Kerättävät tiedot</h2>
            <p>Keräämme seuraavia henkilötietoja palvelun toiminnan mahdollistamiseksi:</p>

            <p><strong>Käyttäjätiedot:</strong></p>
            <ul>
                <li>Nimi</li>
                <li>Sähköpostiosoite</li>
                <li>Paino (kilogrammoina) - käytetään promillelaskentaan</li>
                <li>Sukupuoli - käytetään promillelaskentaan</li>
            </ul>

            <p><strong>Kirjautumistiedot:</strong></p>
            <ul>
                <li>Salasana (salattu BCrypt-algoritmilla)</li>
                <li>Google-tilin tunniste (jos kirjaudut Googlella)</li>
            </ul>

            <p><strong>Käyttötiedot:</strong></p>
            <ul>
                <li>Juomien ajankohdat</li>
                <li>Juomien alkoholimäärät</li>
                <li>Osallistuminen bileisiin</li>
            </ul>

            <h2>3. Tietojen käyttötarkoitus</h2>
            <p>Käytämme henkilötietojasi seuraaviin tarkoituksiin:</p>
            <ul>
                <li>Käyttäjätilin luominen ja hallinta</li>
                <li>Veren alkoholipitoisuuden (promillen) laskeminen painon ja sukupuolen perusteella</li>
                <li>Juomatietojen tallentaminen ja näyttäminen</li>
                <li>Biletilastojen seuranta</li>
            </ul>

            <h2>4. Kolmannen osapuolen palvelut</h2>
            <p>Käytämme seuraavia kolmannen osapuolen palveluita:</p>
            <ul>
                <li><strong>Google Sign-In:</strong> Kirjautumiseen Google-tilillä. Google käsittelee kirjautumistietojasi oman tietosuojakäytäntönsä mukaisesti.</li>
                <li><strong>Gravatar:</strong> Profiilikuvien näyttämiseen sähköpostiosoitteen perusteella. Sähköpostiosoitteesi tiiviste (hash) lähetetään Gravatar-palveluun.</li>
            </ul>

            <h2>5. Tietojen säilytys</h2>
            <ul>
                <li>Käyttäjätiedot säilytetään niin kauan kuin käyttäjätili on aktiivinen</li>
                <li>Vierastilit (guest) ovat väliaikaisia ja voidaan poistaa</li>
                <li>Istuntotiedot säilytetään 90 päivää</li>
            </ul>

            <h2>6. Tietoturva</h2>
            <p>Suojaamme tietojasi seuraavilla tavoilla:</p>
            <ul>
                <li>Salasanat salataan BCrypt-algoritmilla</li>
                <li>Tietoliikenne on suojattu HTTPS-salauksella</li>
                <li>Tietokanta on suojattu palomuurilla ja pääsynhallinnalla</li>
            </ul>

            <h2>7. Oikeutesi</h2>
            <p>Sinulla on seuraavat oikeudet henkilötietoihisi:</p>
            <ul>
                <li><strong>Oikeus saada pääsy tietoihin:</strong> Voit pyytää kopion henkilötiedoistasi</li>
                <li><strong>Oikeus tietojen oikaisemiseen:</strong> Voit päivittää tietojasi profiiliasetuksissa</li>
                <li><strong>Oikeus tietojen poistamiseen:</strong> Voit pyytää tilisi ja tietojesi poistamista</li>
                <li><strong>Oikeus siirtää tiedot:</strong> Voit viedä juomatietosi CSV-muodossa</li>
            </ul>
            <p>Käyttääksesi näitä oikeuksia, ota yhteyttä: privacy@ryyppy.net</p>

            <h2>8. Evästeet</h2>
            <p>
                Käytämme istuntoevästeitä kirjautumisen ja käyttäjäistunnon ylläpitämiseen.
                Nämä evästeet ovat välttämättömiä palvelun toiminnalle.
            </p>

            <h2>9. Muutokset tietosuojaselosteeseen</h2>
            <p>
                Pidätämme oikeuden päivittää tätä tietosuojaselostetta.
                Merkittävistä muutoksista ilmoitetaan palvelussa.
            </p>

            <h2>10. Yhteydenotto</h2>
            <p>
                Jos sinulla on kysyttävää tietosuojasta tai henkilötietojesi käsittelystä,
                ota yhteyttä: privacy@ryyppy.net
            </p>

            <p class="last-updated">Päivitetty: 17.1.2026</p>
        </div>

        <div class="back-link">
            <a href="/ui/login">&larr; Takaisin kirjautumissivulle</a>
        </div>
    </jsp:body>
</t:master>
