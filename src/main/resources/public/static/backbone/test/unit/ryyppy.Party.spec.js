describe('Party module', function () {
    it('Construction', function () {
        var party = new Party();
        expect(party).not.toBeUndefined();
        expect(party.get('name')).toBe('');
    });
});